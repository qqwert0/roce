package roce.table

import common.storage._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import roce._
import common.Collector


class CONN_TABLE() extends Module{
	val io = IO(new Bundle{
		val tx2conn_req	    = Flipped(Decoupled(UInt(24.W)))
		val conn_init	    = Flipped(Decoupled(new CONN_REQ()))
		val conn2tx_rsp	    = (Decoupled(new CONN_STATE()))
	})


    val conn_tx_fifo = Module(new Queue(UInt(24.W), entries=16))
    val conn_init_fifo = Module(new Queue(new CONN_REQ(), entries=16))


    io.tx2conn_req                       <> conn_tx_fifo.io.enq
    io.conn_init                         <> conn_init_fifo.io.enq

    val conn_table = XRam(new CONN_STATE(), CONFIG.MAX_QPS, latency = 1)

    val conn_request = RegInit(0.U.asTypeOf(new CONN_REQ()))

	


	val sIDLE :: sTXRSP :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)
    Collector.report(state===sIDLE, "CONN_TABLE===sIDLE")
    conn_table.io.addr_a                 := 0.U
    conn_table.io.addr_b                 := 0.U
    conn_table.io.wr_en_a                := 0.U
    conn_table.io.data_in_a              := 0.U.asTypeOf(conn_table.io.data_in_a)

    conn_tx_fifo.io.deq.ready                 := state === sIDLE 
    conn_init_fifo.io.deq.ready               := state === sIDLE & (!conn_tx_fifo.io.deq.valid.asBool)


    io.conn2tx_rsp.valid                 := 0.U
    io.conn2tx_rsp.bits                  := 0.U.asTypeOf(io.conn2tx_rsp.bits)
    
	switch(state){
		is(sIDLE){
            when(conn_tx_fifo.io.deq.fire()){
                conn_table.io.addr_b             := conn_tx_fifo.io.deq.bits
                state                           := sTXRSP
            }.elsewhen(conn_init_fifo.io.deq.fire()){
                conn_table.io.addr_a                    := conn_init_fifo.io.deq.bits.qpn
                conn_table.io.wr_en_a                   := 1.U
                conn_table.io.data_in_a.remote_qpn      := conn_init_fifo.io.deq.bits.remote_qpn
                conn_table.io.data_in_a.remote_ip       := conn_init_fifo.io.deq.bits.remote_ip
                conn_table.io.data_in_a.remote_udp_port := conn_init_fifo.io.deq.bits.remote_udp_port
                state                                   := sIDLE
            }.otherwise{
                state                           := sIDLE
            }
		}
		is(sTXRSP){
			when(io.conn2tx_rsp.ready){
				io.conn2tx_rsp.valid 		    := 1.U 
				io.conn2tx_rsp.bits 		        <> conn_table.io.data_out_b
                state                           := sIDLE
			}.otherwise{
                state                           := sTXRSP
            }
		}	
	}

}