package roce.tx_ibh

import common.storage._
import common.axi._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._

class TX_IBH_FSM() extends Module{
	val io = IO(new Bundle{
		val ibh_meta_in         = Flipped(Decoupled(new IBH_META()))

        val psn2tx_rsp          = Flipped(Decoupled(new PSN_STATE()))
        val conn2tx_rsp         = Flipped(Decoupled(new CONN_STATE()))

        val tx2psn_req          = (Decoupled(new PSN_TX_REQ()))
        val tx2conn_req         = (Decoupled(UInt(24.W)))

        val udpip_meta_out	    = (Decoupled(new UDPIP_META()))

        val head_data_out       = (Decoupled(new AXIS(CONFIG.DATA_WIDTH)))


	})

    val psn_tx_fifo = Module(new Queue(new PSN_STATE(), 16))
    val conn_tx_fifo = Module(new Queue(new CONN_STATE(), 16))
    io.psn2tx_rsp                       <> psn_tx_fifo.io.enq
    io.conn2tx_rsp                      <> conn_tx_fifo.io.enq

	val ibh_meta = RegInit(0.U.asTypeOf(new IBH_META()))

    val ibh_head = Wire(new IBH_HEADER())

    ibh_head       := 0.U.asTypeOf(ibh_head)

	val sIDLE :: sGENERATE :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)
    ReporterROCE.report(state===sIDLE, "TX_IBH_FSM===sIDLE")  	
	
	io.ibh_meta_in.ready                    := (state === sIDLE) & io.tx2psn_req.ready & io.tx2conn_req.ready
    psn_tx_fifo.io.deq.ready                := (state === sGENERATE) & conn_tx_fifo.io.deq.valid & io.udpip_meta_out.ready & io.head_data_out.ready
    conn_tx_fifo.io.deq.ready               := (state === sGENERATE) & psn_tx_fifo.io.deq.valid & io.udpip_meta_out.ready & io.head_data_out.ready


    io.tx2psn_req.valid             := 0.U
    io.tx2psn_req.bits              := 0.U.asTypeOf(io.tx2psn_req.bits)
    io.tx2conn_req.valid            := 0.U
    io.tx2conn_req.bits             := 0.U.asTypeOf(io.tx2conn_req.bits)
    io.udpip_meta_out.valid         := 0.U
    io.udpip_meta_out.bits          := 0.U.asTypeOf(io.udpip_meta_out.bits)
    io.head_data_out.valid          := 0.U
    io.head_data_out.bits           := 0.U.asTypeOf(io.head_data_out.bits)

	
	switch(state){
		is(sIDLE){
			when(io.ibh_meta_in.fire()){
				ibh_meta	                    <> io.ibh_meta_in.bits
                io.tx2psn_req.valid             := 1.U
                io.tx2psn_req.bits.qpn          := io.ibh_meta_in.bits.qpn
                when(io.ibh_meta_in.bits.op_code === IB_OP_CODE.RC_READ_REQUEST){
                    io.tx2psn_req.bits.npsn_add := io.ibh_meta_in.bits.num_pkg
                }.elsewhen(PKG_JUDGE.WRITE_PKG(io.ibh_meta_in.bits.op_code)){
                    io.tx2psn_req.bits.npsn_add := 1.U
                }.elsewhen(PKG_JUDGE.READ_RSP_PKG(ibh_meta.op_code)){
                    io.tx2psn_req.bits.rsp_psn  := io.ibh_meta_in.bits.psn
                }                
                io.tx2conn_req.valid            := 1.U
                io.tx2conn_req.bits             := io.ibh_meta_in.bits.qpn
                state                           := sGENERATE
			}
		}
		is(sGENERATE){
            when(psn_tx_fifo.io.deq.fire() & conn_tx_fifo.io.deq.fire()){
                when(PKG_JUDGE.READ_RSP_PKG(ibh_meta.op_code) | ibh_meta.is_retrans | (ibh_meta.op_code === IB_OP_CODE.RC_ACK)){
                    ibh_head.psn                    := ibh_meta.psn
                }.otherwise{
                    ibh_head.psn                    := psn_tx_fifo.io.deq.bits.tx_npsn
                }
                // when(ibh_meta.op_code === IB_OP_CODE.RC_READ_REQUEST){
                //     io.tx2psn_req.valid             := 1.U
                //     io.tx2psn_req.bits.gene_psn(ibh_meta.qpn, psn_tx_fifo.io.deq.bits.rx_epsn, psn_tx_fifo.io.deq.bits.tx_npsn + ibh_meta.num_pkg, psn_tx_fifo.io.deq.bits.tx_old_unack, true.B)
                // }.elsewhen(PKG_JUDGE.WRITE_PKG(ibh_meta.op_code)){
                //     io.tx2psn_req.valid             := 1.U
                //     io.tx2psn_req.bits.gene_psn(ibh_meta.qpn, psn_tx_fifo.io.deq.bits.rx_epsn, psn_tx_fifo.io.deq.bits.tx_npsn + 1.U, psn_tx_fifo.io.deq.bits.tx_old_unack, true.B)
                // }
                ibh_head.qpn                        := conn_tx_fifo.io.deq.bits.remote_qpn
                ibh_head.p_key                      := "hffff".U
                ibh_head.op_code                    := ibh_meta.op_code.asUInt
                when(ibh_meta.op_code===IB_OP_CODE.RC_ACK){
                    ibh_head.ack                    := 1.U
                }.otherwise{
                    ibh_head.ack                    := 0.U
                }                         
                io.head_data_out.valid              := 1.U
                io.head_data_out.bits.data          := ibh_head.asTypeOf(io.head_data_out.bits.data)
                io.head_data_out.bits.keep          := "hffffffffffffffff".U
                io.head_data_out.bits.last          := 1.U
                io.udpip_meta_out.valid             := 1.U
                io.udpip_meta_out.bits.qpn          := ibh_meta.qpn
                io.udpip_meta_out.bits.op_code      := ibh_meta.op_code
                io.udpip_meta_out.bits.dest_ip      := conn_tx_fifo.io.deq.bits.remote_ip
                io.udpip_meta_out.bits.dest_port    := conn_tx_fifo.io.deq.bits.remote_udp_port
                io.udpip_meta_out.bits.udp_length   := ibh_meta.udp_length
                state                               := sIDLE
            }   
		}        
	}

    val tx_ibh_event_cnt = RegInit(0.U(16.W))

    when(io.ibh_meta_in.fire()){
        tx_ibh_event_cnt     := tx_ibh_event_cnt + 1.U
        when(tx_ibh_event_cnt === 1000.U){
            printf(p" tx_ibh_event_cnt: ${tx_ibh_event_cnt} \n");
        }
    }  

}