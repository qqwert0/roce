package roce.cmd_ctrl

import common.storage._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import common.Collector

class EVENT_MERGE() extends Module{
	val io = IO(new Bundle{
		val rx_ack_event        = Flipped(Decoupled(new IBH_META()))
        // val rx_nak_event        = Flipped(Decoupled(new IBH_META()))
        val credit_ack_event    = Flipped(Decoupled(new IBH_META()))
		val remote_read_event	= Flipped(Decoupled(new IBH_META()))
		val tx_local_event	    = Flipped(Decoupled(new IBH_META()))

		val tx_exh_event	    = (Decoupled(new IBH_META()))
        val m_mem_read_cmd	    = (Decoupled(new MEM_CMD()))
        val pkg_info	        = (Decoupled(new PKG_INFO()))
	})

    // val rx_nak_fifo = Module(new Queue(new IBH_META(), 16))
    val rx_ack_fifo = Module(new Queue(new IBH_META(), 64))
    val credit_ack_event = Module(new Queue(new IBH_META(), 64))
    val remote_read_fifo = Module(new Queue(new IBH_META(), 16))
    val tx_local_fifo = Module(new Queue(new IBH_META(), 16))

    val isTx = RegInit(0.B)

    // io.rx_nak_event                     <> rx_nak_fifo.io.enq
    io.rx_ack_event                     <> rx_ack_fifo.io.enq
    io.credit_ack_event                 <> credit_ack_event.io.enq
    io.remote_read_event                <> remote_read_fifo.io.enq
    io.tx_local_event                   <> tx_local_fifo.io.enq

    // rx_nak_fifo.io.deq.ready                := io.tx_exh_event.ready
    rx_ack_fifo.io.deq.ready                := io.tx_exh_event.ready
    credit_ack_event.io.deq.ready           := io.tx_exh_event.ready & (!rx_ack_fifo.io.deq.valid)
    remote_read_fifo.io.deq.ready           := io.tx_exh_event.ready & (!rx_ack_fifo.io.deq.valid) & (!credit_ack_event.io.deq.valid) & io.m_mem_read_cmd.ready & io.pkg_info.ready & (Mux(isTx,(!tx_local_fifo.io.deq.valid),true.B))
    tx_local_fifo.io.deq.ready              := io.tx_exh_event.ready & (!rx_ack_fifo.io.deq.valid) & (!credit_ack_event.io.deq.valid) & io.m_mem_read_cmd.ready & io.pkg_info.ready & (Mux(isTx,true.B,(!remote_read_fifo.io.deq.valid)))

    io.tx_exh_event.valid                   := 0.U
    io.tx_exh_event.bits                    := 0.U.asTypeOf(io.tx_exh_event.bits)

	io.m_mem_read_cmd.bits 		            := 0.U.asTypeOf(io.m_mem_read_cmd.bits)
    io.m_mem_read_cmd.valid 	            := 0.U

 	io.pkg_info.bits 		                := 0.U.asTypeOf(io.pkg_info.bits)
    io.pkg_info.valid 	                    := 0.U   


    // when(rx_nak_fifo.io.deq.fire()){
    //     io.tx_exh_event.valid           := 1.U
    //     io.tx_exh_event.bits            <> rx_nak_fifo.io.deq.bits        
    when(credit_ack_event.io.deq.fire()){
        io.tx_exh_event.valid           := 1.U
        io.tx_exh_event.bits            <> credit_ack_event.io.deq.bits
    }.elsewhen(rx_ack_fifo.io.deq.fire()){
        io.tx_exh_event.valid           := 1.U
        io.tx_exh_event.bits            <> rx_ack_fifo.io.deq.bits
    }.elsewhen(isTx){
        when(tx_local_fifo.io.deq.fire()){
            io.tx_exh_event.valid           := 1.U
            io.tx_exh_event.bits            <> tx_local_fifo.io.deq.bits            
            when(PKG_JUDGE.HAVE_DATA(tx_local_fifo.io.deq.bits.op_code)){
                when(PKG_JUDGE.READ_MEM_PKG(tx_local_fifo.io.deq.bits.op_code)){
                    io.m_mem_read_cmd.valid           := 1.U
                    io.m_mem_read_cmd.bits.vaddr      := tx_local_fifo.io.deq.bits.l_vaddr
                    io.pkg_info.bits.data_from_mem    := true.B                    
                }.otherwise{
                    io.pkg_info.bits.data_from_mem    := false.B                    
                }
                when(tx_local_fifo.io.deq.bits.length > CONFIG.MTU.U){
                    io.m_mem_read_cmd.bits.length     := CONFIG.MTU.U
                    io.pkg_info.bits.pkg_length       := CONFIG.MTU.U
                }.otherwise{
                    io.m_mem_read_cmd.bits.length     := tx_local_fifo.io.deq.bits.length
                    io.pkg_info.bits.pkg_length       := tx_local_fifo.io.deq.bits.length
                }
                when(PKG_JUDGE.RETH_PKG(tx_local_fifo.io.deq.bits.op_code)){
                    io.pkg_info.bits.pkg_type := PKG_TYPE.RETH
                }.otherwise{
                    io.pkg_info.bits.pkg_type := PKG_TYPE.reserve0
                }
                io.pkg_info.valid           := 1.U                                        
            }
            when(PKG_JUDGE.WR_MSG_NOT_LAST_PKG(tx_local_fifo.io.deq.bits.op_code)){
                isTx := true.B
            }.elsewhen(PKG_JUDGE.WR_MSG_LAST_PKG(tx_local_fifo.io.deq.bits.op_code)){
                isTx := false.B
            }
        }.elsewhen(remote_read_fifo.io.deq.fire()){
            io.tx_exh_event.valid           := 1.U
            io.tx_exh_event.bits            <> remote_read_fifo.io.deq.bits
            when(PKG_JUDGE.READ_MEM_PKG(remote_read_fifo.io.deq.bits.op_code)){
                io.m_mem_read_cmd.valid           := 1.U
                io.m_mem_read_cmd.bits.vaddr      := remote_read_fifo.io.deq.bits.vaddr
                io.m_mem_read_cmd.bits.length     := remote_read_fifo.io.deq.bits.length  
                when(PKG_JUDGE.AETH_PKG(remote_read_fifo.io.deq.bits.op_code)){
                    io.pkg_info.bits.pkg_type := PKG_TYPE.AETH
                }.otherwise{
                    io.pkg_info.bits.pkg_type := PKG_TYPE.reserve0
                }
                io.pkg_info.valid := 1.U
                io.pkg_info.bits.pkg_length := remote_read_fifo.io.deq.bits.length  
                io.pkg_info.bits.data_from_mem    := true.B                    
            }
        }
    }.otherwise{
        when(remote_read_fifo.io.deq.fire()){
            io.tx_exh_event.valid           := 1.U
            io.tx_exh_event.bits            <> remote_read_fifo.io.deq.bits
            when(PKG_JUDGE.READ_MEM_PKG(remote_read_fifo.io.deq.bits.op_code)){
                io.m_mem_read_cmd.valid           := 1.U
                io.m_mem_read_cmd.bits.vaddr      := remote_read_fifo.io.deq.bits.vaddr
                io.m_mem_read_cmd.bits.length     := remote_read_fifo.io.deq.bits.length  
                when(PKG_JUDGE.AETH_PKG(remote_read_fifo.io.deq.bits.op_code)){
                    io.pkg_info.bits.pkg_type := PKG_TYPE.AETH
                }.otherwise{
                    io.pkg_info.bits.pkg_type := PKG_TYPE.reserve0
                }
                io.pkg_info.valid := 1.U
                io.pkg_info.bits.pkg_length := remote_read_fifo.io.deq.bits.length   
                io.pkg_info.bits.data_from_mem    := true.B                   
            }
        }.elsewhen(tx_local_fifo.io.deq.fire()){
            io.tx_exh_event.valid           := 1.U
            io.tx_exh_event.bits            <> tx_local_fifo.io.deq.bits
            when(PKG_JUDGE.HAVE_DATA(tx_local_fifo.io.deq.bits.op_code)){
                when(PKG_JUDGE.READ_MEM_PKG(tx_local_fifo.io.deq.bits.op_code)){
                    io.m_mem_read_cmd.valid           := 1.U
                    io.m_mem_read_cmd.bits.vaddr      := tx_local_fifo.io.deq.bits.l_vaddr
                    io.pkg_info.bits.data_from_mem    := true.B                    
                }.otherwise{
                    io.pkg_info.bits.data_from_mem    := false.B                    
                }
                when(tx_local_fifo.io.deq.bits.length > CONFIG.MTU.U){
                    io.m_mem_read_cmd.bits.length     := CONFIG.MTU.U
                    io.pkg_info.bits.pkg_length      := CONFIG.MTU.U
                }.otherwise{
                    io.m_mem_read_cmd.bits.length    := tx_local_fifo.io.deq.bits.length
                    io.pkg_info.bits.pkg_length      := tx_local_fifo.io.deq.bits.length  
                }
                when(PKG_JUDGE.RETH_PKG(tx_local_fifo.io.deq.bits.op_code)){
                    io.pkg_info.bits.pkg_type := PKG_TYPE.RETH
                }.otherwise{
                    io.pkg_info.bits.pkg_type := PKG_TYPE.reserve0
                }
                io.pkg_info.valid           := 1.U                                     
            }
            when(PKG_JUDGE.WR_MSG_NOT_LAST_PKG(tx_local_fifo.io.deq.bits.op_code)){
                isTx := true.B
            }.elsewhen(PKG_JUDGE.WR_MSG_LAST_PKG(tx_local_fifo.io.deq.bits.op_code)){
                isTx := false.B
            }
        }
    }
    
    
    


}