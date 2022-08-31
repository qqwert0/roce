package roce.rx_exh

import common.storage._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import common.BaseILA
import common.Collector


class RX_EXH_FSM() extends Module{
	val io = IO(new Bundle{
		val ibh_meta_in         = Flipped(Decoupled(new IBH_META()))

        val msn2rx_rsp          = Flipped(Decoupled(new MSN_STATE()))
        val l_read_req_pop_rsp  = Flipped(Decoupled(new MQ_POP_RSP(UInt(64.W))))

		val m_mem_write_cmd     = (Decoupled(new MEM_CMD()))
        val m_recv_meta         = (Decoupled(new RECV_META()))

        val rx2msn_req          = (Decoupled(new MSN_REQ()))
        val rx2fc_req           = (Decoupled(new FC_REQ()))
        val l_read_req_pop_req  = (Decoupled(UInt(24.W)))
        val r_read_req_req      = (Decoupled(new RD_REQ()))
        val rq_req	            = (Decoupled(new CMPT_META()))

        val ack_event	        = (Decoupled(new IBH_META()))

        val pkg_type2exh        = (Decoupled(new RX_PKG_INFO()))
        val pkg_type2mem        = (Decoupled(new RX_PKG_INFO()))
	})

    val ibh_meta_fifo = Module(new Queue(new IBH_META(), 16))
    val msn_rx_fifo = Module(new Queue(new MSN_STATE(), 16))
    val l_read_pop_fifo = Module(new Queue(new MQ_POP_RSP(UInt(64.W)), 16))

    Collector.fire(io.msn2rx_rsp)

    io.ibh_meta_in                      <> ibh_meta_fifo.io.enq
    io.msn2rx_rsp                       <> msn_rx_fifo.io.enq
    io.l_read_req_pop_rsp               <> l_read_pop_fifo.io.enq

	val ibh_meta = RegInit(0.U.asTypeOf(new IBH_META()))
    val msn_meta = RegInit(0.U.asTypeOf(new MSN_STATE()))
    val l_read_addr = RegInit(0.U(64.W))
    val consume_read_addr = RegInit(false.B)
    val num_pkg_total = RegInit(0.U(21.W))

    val payload_length = WireInit(0.U(16.W))
    val remain_length = WireInit(0.U(32.W))
    val credit_tmp = WireInit(0.U(13.W))
    

	val sIDLE :: sMETA :: sDATA :: Nil = Enum(3)
	val state                   = RegInit(sIDLE)	
    Collector.report(state===sIDLE, "RX_EXH_FSM===sIDLE")


	ibh_meta_fifo.io.deq.ready      := (state === sIDLE) & Mux((ibh_meta_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_READ_RESP_FIRST | ibh_meta_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_READ_RESP_ONLY) , (io.rx2msn_req.ready & io.l_read_req_pop_req.ready) , io.rx2msn_req.ready )
    msn_rx_fifo.io.deq.ready        := (state === sMETA) & Mux((ibh_meta.op_code === IB_OP_CODE.RC_READ_REQUEST), io.r_read_req_req.ready , ((~consume_read_addr) | l_read_pop_fifo.io.deq.valid) & io.pkg_type2exh.ready & io.pkg_type2mem.ready & io.ack_event.ready & io.m_mem_write_cmd.ready & io.m_recv_meta.ready & io.rx2fc_req.ready & io.r_read_req_req.ready)
    l_read_pop_fifo.io.deq.ready    := (state === sMETA) & Mux((ibh_meta.op_code === IB_OP_CODE.RC_READ_REQUEST), io.r_read_req_req.ready , msn_rx_fifo.io.deq.valid & io.pkg_type2exh.ready & io.pkg_type2mem.ready & io.ack_event.ready & io.m_mem_write_cmd.ready & io.m_recv_meta.ready & io.rx2fc_req.ready & io.r_read_req_req.ready)


    Collector.report(consume_read_addr)
    Collector.report(io.pkg_type2exh.ready)
    Collector.report(io.pkg_type2mem.ready)
    Collector.report(io.ack_event.ready)
    Collector.report(io.m_mem_write_cmd.ready)
    Collector.report(io.m_recv_meta.ready)
    Collector.report(io.rx2fc_req.ready)
    Collector.report(io.r_read_req_req.ready)

    io.m_mem_write_cmd.valid        := 0.U
    io.m_mem_write_cmd.bits         := 0.U.asTypeOf(io.m_mem_write_cmd.bits)
    io.m_recv_meta.valid            := 0.U
    io.m_recv_meta.bits             := 0.U.asTypeOf(io.m_recv_meta.bits)    
    io.rx2msn_req.valid             := 0.U
    io.rx2msn_req.bits              := 0.U.asTypeOf(io.rx2msn_req.bits)
    io.rx2fc_req.valid              := 0.U
    io.rx2fc_req.bits               := 0.U.asTypeOf(io.rx2fc_req.bits)    
    io.l_read_req_pop_req.valid     := 0.U
    io.l_read_req_pop_req.bits      := 0.U.asTypeOf(io.l_read_req_pop_req.bits)
    io.r_read_req_req.valid         := 0.U
    io.r_read_req_req.bits          := 0.U.asTypeOf(io.r_read_req_req.bits)
    io.rq_req.valid                 := 0.U
    io.rq_req.bits                  := 0.U.asTypeOf(io.rq_req.bits)    
    io.ack_event.valid              := 0.U
    io.ack_event.bits               := 0.U.asTypeOf(io.ack_event.bits)
    io.pkg_type2exh.valid           := 0.U
    io.pkg_type2exh.bits            := 0.U.asTypeOf(io.pkg_type2exh.bits)
    io.pkg_type2mem.valid           := 0.U
    io.pkg_type2mem.bits            := 0.U.asTypeOf(io.pkg_type2mem.bits)

	
	switch(state){
		is(sIDLE){
			when(ibh_meta_fifo.io.deq.fire()){
				ibh_meta	                    <> ibh_meta_fifo.io.deq.bits
                io.rx2msn_req.valid             := 1.U
                io.rx2msn_req.bits.qpn          := ibh_meta_fifo.io.deq.bits.qpn               
                consume_read_addr               := false.B
                num_pkg_total                   := (ibh_meta_fifo.io.deq.bits.length + CONFIG.MTU.U-1.U) / CONFIG.MTU.U
                when(ibh_meta_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_READ_RESP_FIRST | ibh_meta_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_READ_RESP_ONLY){
                    consume_read_addr           := true.B
                    io.l_read_req_pop_req.valid := 1.U
                    io.l_read_req_pop_req.bits  := ibh_meta_fifo.io.deq.bits.qpn
                }
                state                           := sMETA
			}
		}
		is(sMETA){
            when(msn_rx_fifo.io.deq.fire() & (!consume_read_addr | l_read_pop_fifo.io.deq.fire())){
                msn_meta                        <> msn_rx_fifo.io.deq.bits
                when(consume_read_addr){
                    l_read_addr                 <> l_read_pop_fifo.io.deq.bits.data
                    consume_read_addr           := false.B
                }
                state                       := sDATA
                
            }
		}        
		is(sDATA){
            switch(ibh_meta.op_code){
                is(IB_OP_CODE.RC_WRITE_FIRST){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-16.U-4.U //UDP, BTH, RETH, CRC
                    remain_length                   := ibh_meta.length - payload_length
                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := ibh_meta.vaddr
                    io.m_mem_write_cmd.bits.length  := payload_length

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn+1.U, ibh_meta.vaddr+payload_length, remain_length, 0.U, true.B)

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)
                    printf(p"${credit_tmp}\n")
                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RETH
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RETH  
                    io.pkg_type2mem.bits.data_to_mem:= true.B  
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE                  
                }
                is(IB_OP_CODE.RC_WRITE_ONLY){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-16.U-4.U //UDP, BTH, RETH, CRC
                    remain_length                   := ibh_meta.length - payload_length
                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := ibh_meta.vaddr
                    io.m_mem_write_cmd.bits.length  := payload_length

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn+1.U, ibh_meta.vaddr+payload_length, remain_length, 0.U, true.B)

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RETH
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RETH  
                    io.pkg_type2mem.bits.data_to_mem:= true.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE                  
                }                
                is(IB_OP_CODE.RC_WRITE_MIDDLE ){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U //UDP, BTH, CRC
                    remain_length                   := msn_meta.length - payload_length
                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := msn_meta.vaddr
                    io.m_mem_write_cmd.bits.length  := payload_length

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn, msn_meta.vaddr+payload_length, remain_length, 0.U, true.B)

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RAW
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RAW 
                    io.pkg_type2mem.bits.data_to_mem:= true.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE    
                }
                is( IB_OP_CODE.RC_WRITE_LAST){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U //UDP, BTH, CRC
                    remain_length                   := msn_meta.length - payload_length
                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := msn_meta.vaddr
                    io.m_mem_write_cmd.bits.length  := payload_length

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn, msn_meta.vaddr+payload_length, remain_length, 0.U, true.B)

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)
                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RAW
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RAW 
                    io.pkg_type2mem.bits.data_to_mem:= true.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE    
                }                
                is(IB_OP_CODE.RC_READ_REQUEST){
                    io.r_read_req_req.valid         := 1.U
                    io.r_read_req_req.bits.qpn      := ibh_meta.qpn
                    io.r_read_req_req.bits.vaddr    := ibh_meta.vaddr
                    io.r_read_req_req.bits.length   := ibh_meta.length
                    io.r_read_req_req.bits.psn      := ibh_meta.psn
                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn+1.U, msn_meta.vaddr, msn_meta.length, 0.U, true.B)  
                    // io.rx2fc_req.valid              := 1.U
                    // io.rx2fc_req.bits.fc_req_generate(ibh_meta.qpn, IB_OP_CODE.RC_READ_REQUEST, ibh_meta.length>>6.U)
                    state                           := sIDLE
                }
                is(IB_OP_CODE.RC_READ_RESP_ONLY){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U-4.U //UDP, BTH, AETH, CRCotherwise{
                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := l_read_addr
                    io.m_mem_write_cmd.bits.length  := payload_length
                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn, l_read_addr+payload_length, 0.U, 0.U, true.B)                        

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)
                    io.rq_req.valid                 := 1.U
                    io.rq_req.bits.cmpt_meta_generate(ibh_meta.qpn,0.U,0.U)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.AETH
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.AETH 
                    io.pkg_type2mem.bits.data_to_mem:= true.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE
                }
                is(IB_OP_CODE.RC_READ_RESP_FIRST){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U-4.U //UDP, BTH, AETH, CRC
                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := l_read_addr
                    io.m_mem_write_cmd.bits.length  := payload_length
                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn, l_read_addr+payload_length, 0.U, 0.U, true.B)                        

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.AETH
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.AETH 
                    io.pkg_type2mem.bits.data_to_mem:= true.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE
                }
                is(IB_OP_CODE.RC_READ_RESP_LAST){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U-4.U //UDP, BTH, AETH, CRC
                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := msn_meta.vaddr
                    io.m_mem_write_cmd.bits.length  := payload_length

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)
                    io.rq_req.valid                 := 1.U
                    io.rq_req.bits.cmpt_meta_generate(ibh_meta.qpn,0.U,0.U)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.AETH
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.AETH 
                    io.pkg_type2mem.bits.data_to_mem:= true.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE
                }                                
                is(IB_OP_CODE.RC_READ_RESP_MIDDLE){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U //UDP, BTH, CRC

                    io.m_mem_write_cmd.valid        := 1.U
                    io.m_mem_write_cmd.bits.vaddr   := msn_meta.vaddr
                    io.m_mem_write_cmd.bits.length  := payload_length
                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn, msn_meta.vaddr+payload_length, 0.U, 0.U, true.B)                        

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RAW
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RAW 
                    io.pkg_type2mem.bits.data_to_mem:= true.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE
                }
                is(IB_OP_CODE.RC_ACK){
                    io.rx2fc_req.valid              := 1.U
                    io.rx2fc_req.bits.fc_req_generate(ibh_meta.qpn, IB_OP_CODE.RC_ACK, ibh_meta.credit, ibh_meta.psn, ibh_meta.is_wr_ack)
                    state                           := sIDLE
                }
                is(IB_OP_CODE.RC_DIRECT_FIRST){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-16.U-4.U //UDP, BTH, RETH, CRC
                    remain_length                   := ibh_meta.length - payload_length

                    io.m_recv_meta.valid            := 1.U
                    io.m_recv_meta.bits.recv_meta_generate(ibh_meta.qpn,msn_meta.msn+1.U,1.U,num_pkg_total)//fix me add pkg_num pkg total msg num

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn+1.U, ibh_meta.vaddr+payload_length, remain_length, 0.U, true.B)
                    io.rx2msn_req.bits.pkg_num      := 1.U
                    io.rx2msn_req.bits.pkg_total    := num_pkg_total

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)
                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RETH
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RETH  
                    io.pkg_type2mem.bits.data_to_mem:= false.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE                  
                }
                is(IB_OP_CODE.RC_DIRECT_ONLY){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-16.U-4.U //UDP, BTH, RETH, CRC
                    remain_length                   := ibh_meta.length - payload_length

                    io.m_recv_meta.valid            := 1.U
                    io.m_recv_meta.bits.recv_meta_generate(ibh_meta.qpn,msn_meta.msn+1.U,1.U,1.U)//fix me add pkg_num pkg total msg num

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn+1.U, ibh_meta.vaddr+payload_length, remain_length, 0.U, true.B)

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RETH
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RETH  
                    io.pkg_type2mem.bits.data_to_mem:= false.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE                  
                }                
                is(IB_OP_CODE.RC_DIRECT_MIDDLE ){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U //UDP, BTH, CRC
                    remain_length                   := msn_meta.length - payload_length

                    io.m_recv_meta.valid            := 1.U
                    io.m_recv_meta.bits.recv_meta_generate(ibh_meta.qpn,msn_meta.msn,msn_meta.pkg_num+1.U,msn_meta.pkg_total)//fix me add pkg_num pkg total msg num

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn, msn_meta.vaddr+payload_length, remain_length, 0.U, true.B)
                    io.rx2msn_req.bits.pkg_num      := msn_meta.pkg_num+1.U
                    io.rx2msn_req.bits.pkg_total    := msn_meta.pkg_total                    

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)

                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RAW
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RAW 
                    io.pkg_type2mem.bits.data_to_mem:= false.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE    
                }
                is( IB_OP_CODE.RC_DIRECT_LAST){
                    payload_length                  := ibh_meta.udp_length -8.U-12.U-4.U //UDP, BTH, CRC
                    remain_length                   := msn_meta.length - payload_length

                    io.m_recv_meta.valid            := 1.U
                    io.m_recv_meta.bits.recv_meta_generate(ibh_meta.qpn,msn_meta.msn,msn_meta.pkg_num+1.U,msn_meta.pkg_total)//fix me add pkg_num pkg total msg num

                    io.rx2msn_req.valid             := 1.U 
                    io.rx2msn_req.bits.msn_req_generate(ibh_meta.qpn, msn_meta.msn, msn_meta.vaddr+payload_length, remain_length, 0.U, true.B)
                    io.rx2msn_req.bits.pkg_num      := msn_meta.pkg_num+1.U
                    io.rx2msn_req.bits.pkg_total    := msn_meta.pkg_total                       

                    io.ack_event.valid              := 1.U
                    credit_tmp                      := payload_length>>6.U
                    io.ack_event.bits.ack_event(ibh_meta.qpn, ibh_meta.psn, credit_tmp, ibh_meta.is_wr_ack)
                    io.pkg_type2exh.valid           := 1.U
                    io.pkg_type2exh.bits.pkg_type   := PKG_TYPE.RAW
                    io.pkg_type2mem.valid           := 1.U
                    io.pkg_type2mem.bits.pkg_type   := PKG_TYPE.RAW 
                    io.pkg_type2mem.bits.data_to_mem:= false.B
                    io.pkg_type2mem.bits.length     := payload_length
                    state                           := sIDLE    
                }                
            }
		}
	}
    

}