package roce.table

import common.storage._
import common._
import common.ToZero
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import roce._

class FC_TABLE() extends Module{
	val io = IO(new Bundle{
		val rx2fc_req  	= Flipped(Decoupled(new FC_REQ()))

		val tx2fc_req	= Flipped(Decoupled(new FC_REQ()))
		val fc_init	    = Flipped(Decoupled(new FC_REQ()))
        val buffer_cnt	= Input(UInt(16.W))
        val ack_event   = (Decoupled(new IBH_META()))
		val fc2tx_rsp	= (Decoupled(new FC_RSP()))
        val status_reg  = Output(Vec(2,UInt(32.W)))
	})

    val fc_rx_fifo = Module(new Queue(new FC_REQ(), entries=16))
    val fc_tx_fifo = Module(new Queue(new FC_REQ(), entries=16))
    val fc_init_fifo = Module(new Queue(new FC_REQ(), entries=16))

    io.rx2fc_req                       <> fc_rx_fifo.io.enq
    io.tx2fc_req                       <> fc_tx_fifo.io.enq
    io.fc_init                         <> fc_init_fifo.io.enq


       

    val fc_table = XRam(new FC_STATE(), CONFIG.MAX_QPS, latency = 1)	

    val tx_fc_request = RegInit(0.U.asTypeOf(new FC_REQ()))
    val rx_fc_request = RegInit(0.U.asTypeOf(new FC_REQ()))
    val rx_tmp_credit = RegInit(0.U.asTypeOf(new FC_STATE()))
    val tmp_credit = RegInit(0.U.asTypeOf(new FC_STATE()))
    val tmp_request = RegInit(0.U.asTypeOf(new FC_REQ()))
    val tx_event_lock = RegInit(0.B)


	val sIDLE :: sTXRSP1 :: sTXRSP2 :: sTXRSP3 :: sTXRSP4 :: sRXRSP :: sRXRSP1 :: Nil = Enum(7)
	val state                   = RegInit(sIDLE)
    ReporterROCE.report(state===sIDLE, "FC_TABLE===sIDLE") 
    fc_table.io.addr_a                 := 0.U
    fc_table.io.addr_b                 := 0.U
    fc_table.io.wr_en_a                := 0.U
    fc_table.io.data_in_a              := 0.U.asTypeOf(fc_table.io.data_in_a)

    fc_rx_fifo.io.deq.ready                 := state === sIDLE
    fc_tx_fifo.io.deq.ready                 := state === sIDLE & (!fc_rx_fifo.io.deq.valid.asBool) & (~tx_event_lock) 
    fc_init_fifo.io.deq.ready               := state === sIDLE & (!fc_rx_fifo.io.deq.valid.asBool) & (!fc_tx_fifo.io.deq.valid.asBool) & (~tx_event_lock)

    io.ack_event.valid                 := 0.U
    io.ack_event.bits                  := 0.U.asTypeOf(io.ack_event.bits)

    io.fc2tx_rsp.valid                 := 0.U
    io.fc2tx_rsp.bits                  := 0.U.asTypeOf(io.fc2tx_rsp.bits)
    
	switch(state){
		is(sIDLE){
            when(fc_init_fifo.io.deq.fire()){
                fc_table.io.addr_a              := fc_init_fifo.io.deq.bits.qpn
                fc_table.io.wr_en_a             := 1.U
                fc_table.io.data_in_a.credit    := fc_init_fifo.io.deq.bits.credit
                state                           := sIDLE
            }.elsewhen(fc_rx_fifo.io.deq.fire()){
                rx_fc_request                      <> fc_rx_fifo.io.deq.bits
                when(fc_rx_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_ACK){
                    fc_table.io.addr_b              := fc_rx_fifo.io.deq.bits.qpn
                    state                           := sRXRSP                      
                }
            }.elsewhen(tx_event_lock){
                fc_table.io.addr_b              := tmp_request.qpn
                state                           := sTXRSP3
            }.elsewhen(fc_tx_fifo.io.deq.fire()){
                tx_fc_request                   <> fc_tx_fifo.io.deq.bits
                fc_table.io.addr_b              := fc_tx_fifo.io.deq.bits.qpn
                when(fc_tx_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_ACK){
                    when(io.buffer_cnt < CONFIG.RX_BUFFER_FULL.U){
                        state                   := sTXRSP1
                    }.otherwise{
                        state                   := sTXRSP2
                    }
                }.elsewhen(PKG_JUDGE.HAVE_DATA(fc_tx_fifo.io.deq.bits.op_code)){
                    state                       := sTXRSP3
                }.otherwise{
                    state                       := sTXRSP1
                }
            }.otherwise{
                state                           := sIDLE
            }
		}
		is(sTXRSP1){
			when(io.fc2tx_rsp.ready){
				io.fc2tx_rsp.valid 		        := 1.U 
                io.fc2tx_rsp.bits.valid_event 	:= true.B 
                state                           := sIDLE
			}.otherwise{
                state                           := sTXRSP1
            }
		}
		is(sTXRSP2){
			when(io.fc2tx_rsp.ready & io.ack_event.ready){
				io.fc2tx_rsp.valid 		        := 1.U 
                io.fc2tx_rsp.bits.valid_event 	:= false.B 
				io.ack_event.valid 		        := 1.U 
				io.ack_event.bits.ack_event(tx_fc_request.qpn, tx_fc_request.credit, tx_fc_request.psn, tx_fc_request.is_wr_ack)
                state                           := sIDLE
			}.otherwise{
                state                           := sTXRSP2
            }
		}
		is(sTXRSP3){
            tmp_credit.credit               := fc_table.io.data_out_b.credit
            tmp_request                     := tx_fc_request
            tx_event_lock                   := true.B
            state                           := sTXRSP4

		}       
		is(sTXRSP4){
			when(io.fc2tx_rsp.ready & (tmp_credit.credit >= tmp_request.credit)){
				io.fc2tx_rsp.valid 		        := 1.U 
                io.fc2tx_rsp.bits.valid_event 	:= true.B 
                state                           := sIDLE
                tx_event_lock                   := false.B
                when(tmp_request.op_code === IB_OP_CODE.RC_WRITE_FIRST){
                    fc_table.io.addr_a              := tmp_request.qpn
                    fc_table.io.wr_en_a             := 1.U
                    fc_table.io.data_in_a.credit    := tmp_credit.credit - CONFIG.MTU_WORD.U                      
                }.otherwise{
                    fc_table.io.addr_a              := tmp_request.qpn
                    fc_table.io.wr_en_a             := 1.U
                    fc_table.io.data_in_a.credit    := tmp_credit.credit  - tmp_request.credit                     
                }
			}.otherwise{
                tx_event_lock                   := true.B
                state                           := sIDLE
            }
		}                  
		is(sRXRSP){
            rx_tmp_credit.credit            := fc_table.io.data_out_b.credit
            state                           := sRXRSP1			
		}
		is(sRXRSP1){
            fc_table.io.addr_a              := rx_fc_request.qpn
            fc_table.io.wr_en_a             := 1.U
            fc_table.io.data_in_a.credit    := rx_tmp_credit.credit + rx_fc_request.credit
            state                           := sIDLE			
		}        	
	}
    
    io.status_reg(0)          := XCounter.record_signals_sync(fc_rx_fifo.io.deq.fire())
    io.status_reg(1)          := XCounter.record_signals_sync(fc_tx_fifo.io.deq.fire())

  	// class ila_fc_table(seq:Seq[Data]) extends BaseILA(seq)
  	// val mod_fc_table = Module(new ila_fc_table(Seq(	
	// 	state,
	//   	io.status_reg(120),
    // 	io.status_reg(121),
    // 	fc_table.io.addr_b,
    //     fc_table.io.data_out_b.credit,
    //     fc_table.io.data_in_a.credit,
    //     fc_table.io.wr_en_a,
    //     fc_rx_fifo.io.deq.bits.op_code,
    //     fc_tx_fifo.io.deq.bits.op_code
  	// )))
  	// mod_fc_table.connect(clock)

}