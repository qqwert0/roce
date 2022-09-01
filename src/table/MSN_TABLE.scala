package roce.table

import common.storage._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import roce._
import common.Collector


class MSN_TABLE() extends Module{
	val io = IO(new Bundle{
		val rx2msn_req  	= Flipped(Decoupled(new MSN_REQ()))

		val tx2msn_req	= Flipped(Decoupled(new MSN_REQ()))
		val msn_init	= Flipped(Decoupled(new MSN_INIT()))
		val msn2rx_rsp	= (Decoupled(new MSN_STATE()))
		val msn2tx_rsp	= (Decoupled(new MSN_STATE()))
	})

    val msn_rx_fifo = Module(new Queue(new MSN_REQ(), entries=16))
    val msn_tx_fifo = Module(new Queue(new MSN_REQ(), entries=16))
    val msn_init_fifo = Module(new Queue(new MSN_INIT(), entries=16))

    io.rx2msn_req                       <> msn_rx_fifo.io.enq
    io.tx2msn_req                       <> msn_tx_fifo.io.enq
    io.msn_init                         <> msn_init_fifo.io.enq

    val msn_table = XRam(new MSN_STATE(), CONFIG.MAX_QPS, latency = 1)

    val msn_request = RegInit(0.U.asTypeOf(new MSN_REQ()))
    
    

	val sIDLE :: sTXRSP :: sRXRSP :: Nil = Enum(3)
	val state                   = RegInit(sIDLE)
    Collector.report(state===sIDLE, "MSN_TABLE===sIDLE")  
    msn_table.io.addr_a                 := 0.U
    msn_table.io.addr_b                 := 0.U
    msn_table.io.wr_en_a                := 0.U
    msn_table.io.data_in_a              := 0.U.asTypeOf(msn_table.io.data_in_a)

    msn_rx_fifo.io.deq.ready                 := state === sIDLE
    msn_tx_fifo.io.deq.ready                 := state === sIDLE & (!msn_rx_fifo.io.deq.valid.asBool) 
    msn_init_fifo.io.deq.ready               := state === sIDLE & (!msn_rx_fifo.io.deq.valid.asBool) & (!msn_tx_fifo.io.deq.valid.asBool)

    io.msn2rx_rsp.valid                 := 0.U
    io.msn2rx_rsp.bits                  := 0.U.asTypeOf(io.msn2rx_rsp.bits)

    io.msn2tx_rsp.valid                 := 0.U
    io.msn2tx_rsp.bits                  := 0.U.asTypeOf(io.msn2tx_rsp.bits)
    
	switch(state){
		is(sIDLE){
            when(msn_rx_fifo.io.deq.fire()){
                when(msn_rx_fifo.io.deq.bits.write){
                    msn_table.io.addr_a             := msn_rx_fifo.io.deq.bits.qpn
                    msn_table.io.wr_en_a            := 1.U
                    msn_table.io.data_in_a.msn      := msn_rx_fifo.io.deq.bits.msn
                    msn_table.io.data_in_a.vaddr    := msn_rx_fifo.io.deq.bits.vaddr
                    msn_table.io.data_in_a.length   := msn_rx_fifo.io.deq.bits.length                      
                    msn_table.io.data_in_a.r_key    := msn_rx_fifo.io.deq.bits.r_key
                    msn_table.io.data_in_a.pkg_num  := msn_rx_fifo.io.deq.bits.pkg_num
                    msn_table.io.data_in_a.pkg_total:= msn_rx_fifo.io.deq.bits.pkg_total                        
                    state                           := sIDLE
                }.otherwise{
                    msn_table.io.addr_b             := msn_rx_fifo.io.deq.bits.qpn
                    state                           := sRXRSP
                }
            }.elsewhen(msn_tx_fifo.io.deq.fire()){
                msn_table.io.addr_b             := msn_tx_fifo.io.deq.bits.qpn
                state                           := sTXRSP
            }.elsewhen(msn_init_fifo.io.deq.fire()){
                msn_table.io.addr_a             := msn_init_fifo.io.deq.bits.qpn
                msn_table.io.wr_en_a            := 1.U
                msn_table.io.data_in_a.msn      := msn_init_fifo.io.deq.bits.msn
                msn_table.io.data_in_a.r_key    := msn_init_fifo.io.deq.bits.r_key
                state                           := sIDLE
            }.otherwise{
                state                           := sIDLE
            }
		}
		is(sTXRSP){
			when(io.msn2tx_rsp.ready){
				io.msn2tx_rsp.valid 		    := 1.U 
				io.msn2tx_rsp.bits 		        <> msn_table.io.data_out_b
                state                           := sIDLE
			}.otherwise{
                state                           := sTXRSP
            }
		}
		is(sRXRSP){
			when(io.msn2rx_rsp.ready){
				io.msn2rx_rsp.valid 		    := 1.U 
				io.msn2rx_rsp.bits 		        <> msn_table.io.data_out_b
                state                           := sIDLE
			}.otherwise{
                state                           := sRXRSP
            }			
		}	
	}

}