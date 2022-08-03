package roce.rx_ibh

import common.storage._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._

class RX_IBH_FSM() extends Module{
	val io = IO(new Bundle{
		val ibh_meta_in         = Flipped(Decoupled(new IBH_META()))

        val psn2rx_rsp          = Flipped(Decoupled(new PSN_STATE()))

        val rx2psn_req          = (Decoupled(new PSN_RX_REQ()))

        val ibh_meta_out	    = (Decoupled(new IBH_META()))
        val drop_info_out	    = (Decoupled(Bool()))
        // val nak_event_out       = (Decoupled(new IBH_META()))        


	})

	val ibh_meta_fifo = Module(new Queue(new IBH_META(),16))
	io.ibh_meta_in 		    <> ibh_meta_fifo.io.enq

    val psn_rx_fifo = Module(new Queue(new PSN_STATE(), 16))
    io.psn2rx_rsp                       <> psn_rx_fifo.io.enq

	val ibh_meta = RegInit(0.U.asTypeOf(new IBH_META()))

	val sIDLE :: sPROCESS :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)
    ReporterROCE.report(state===sIDLE, "RX_IBH_FSM===sIDLE")		
	
	ibh_meta_fifo.io.deq.ready                    := (state === sIDLE) & io.rx2psn_req.ready
    psn_rx_fifo.io.deq.ready                := (state === sPROCESS) & io.ibh_meta_out.ready & io.drop_info_out.ready //& io.nak_event_out.ready


    io.rx2psn_req.valid                 := 0.U
    io.rx2psn_req.bits                  := 0.U.asTypeOf(io.rx2psn_req.bits)

    io.ibh_meta_out.valid               := 0.U
    io.ibh_meta_out.bits                := 0.U.asTypeOf(io.ibh_meta_out.bits)
    io.drop_info_out.valid              := 0.U
    io.drop_info_out.bits               := 0.U.asTypeOf(io.drop_info_out.bits)
    // io.nak_event_out.valid              := 0.U
    // io.nak_event_out.bits               := 0.U.asTypeOf(io.nak_event_out.bits)  
	switch(state){
		is(sIDLE){
			when(ibh_meta_fifo.io.deq.fire()){
				ibh_meta	                    <> ibh_meta_fifo.io.deq.bits
                io.rx2psn_req.valid             := 1.U
                io.rx2psn_req.bits.qpn          := ibh_meta_fifo.io.deq.bits.qpn
                io.rx2psn_req.bits.rx_psn       := ibh_meta_fifo.io.deq.bits.psn
                io.rx2psn_req.bits.op_code      := ibh_meta_fifo.io.deq.bits.op_code
                when(ibh_meta_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_ACK){
                    when(ibh_meta_fifo.io.deq.bits.isNAK){
                        io.rx2psn_req.bits.is_nak := true.B   
                    }
                }
                state                           := sPROCESS
			}
		}
		is(sPROCESS){
            when(psn_rx_fifo.io.deq.fire()){
                io.ibh_meta_out.valid               := 1.U
                io.ibh_meta_out.bits                := ibh_meta
                when(ibh_meta.op_code === IB_OP_CODE.RC_ACK){   
                    io.drop_info_out.valid          := 1.U
                    io.drop_info_out.bits           := false.B 
                }.elsewhen(PKG_JUDGE.REQ_PKG(ibh_meta.op_code)){
                    when(ibh_meta.psn === psn_rx_fifo.io.deq.bits.rx_epsn){
                        // io.rx2psn_req.valid             := 1.U
                        // io.rx2psn_req.bits.gene_psn(ibh_meta.qpn, ibh_meta.psn+1.U, psn_rx_fifo.io.deq.bits.tx_npsn, psn_rx_fifo.io.deq.bits.tx_old_unack, true.B)                       
                        io.drop_info_out.valid          := 1.U
                        io.drop_info_out.bits           := false.B                                    
                    }.elsewhen(ibh_meta.psn < psn_rx_fifo.io.deq.bits.rx_epsn){
                        io.drop_info_out.valid          := 1.U
                        io.drop_info_out.bits           := true.B   
                    }otherwise{                    
                        io.drop_info_out.valid          := 1.U
                        io.drop_info_out.bits           := true.B 
                        // io.nak_event_out.valid          := 1.U
                        // io.nak_event_out.bits.nak_event(ibh_meta.qpn, psn_rx_fifo.io.deq.bits.rx_epsn, true.B) 
                    }  
                }.otherwise{
                    when(ibh_meta.psn === psn_rx_fifo.io.deq.bits.tx_old_unack){
                        io.drop_info_out.valid          := 1.U
                        io.drop_info_out.bits           := false.B                                    
                    }.elsewhen(ibh_meta.psn < psn_rx_fifo.io.deq.bits.tx_old_unack){
                        io.drop_info_out.valid          := 1.U
                        io.drop_info_out.bits           := true.B   
                    }otherwise{                    
                        io.drop_info_out.valid          := 1.U
                        io.drop_info_out.bits           := true.B 
                        // io.nak_event_out.valid          := 1.U
                        // io.nak_event_out.bits.nak_event(ibh_meta.qpn, psn_rx_fifo.io.deq.bits.rx_epsn, false.B) 
                    }                    
                }

                state                               := sIDLE  
            }   
		}        
	}
}