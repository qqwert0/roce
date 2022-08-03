package roce.rx_ibh

import common.storage._
import common.axi._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._

class RX_EXH_PROCESS() extends Module{
	val io = IO(new Bundle{
		val rx_exh_data_in      = Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
        val ibh_meta_in       	= Flipped(Decoupled(new IBH_META()))
		val ibh_meta_out	    = (Decoupled(new IBH_META()))
		val rx_exh_data_out	    = (Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
	})

	val ibh_meta_fifo = Module(new Queue(new IBH_META(),16))
	val exh_data_fifo = Module(new Queue(new AXIS(CONFIG.DATA_WIDTH),16))
	io.ibh_meta_in 			<> ibh_meta_fifo.io.enq
	io.rx_exh_data_in 		<> exh_data_fifo.io.enq


	val reth_header_tmp = Wire(new RETH_HEADER())
	val aeth_header_tmp = Wire(new AETH_HEADER())
    reth_header_tmp                 := 0.U.asTypeOf(reth_header_tmp)
	aeth_header_tmp                 := 0.U.asTypeOf(aeth_header_tmp)

	val sIDLE :: sPAYLOAD :: Nil = Enum(2)
	val state                       = RegInit(sIDLE)
	ReporterROCE.report(state===sIDLE, "RX_EXH_PROCESS===sIDLE")	
	
	exh_data_fifo.io.deq.ready         := ((state === sIDLE) & ibh_meta_fifo.io.deq.valid & io.ibh_meta_out.ready & io.rx_exh_data_out.ready) | ((state === sPAYLOAD) & io.rx_exh_data_out.ready) 

	ibh_meta_fifo.io.deq.ready          	:= (state === sIDLE) & exh_data_fifo.io.deq.valid & io.ibh_meta_out.ready & io.rx_exh_data_out.ready


	io.ibh_meta_out.valid 			:= 0.U 
	io.ibh_meta_out.bits 		    := 0.U.asTypeOf(io.ibh_meta_out.bits)
	io.rx_exh_data_out.valid 		:= 0.U 
	io.rx_exh_data_out.bits 		:= 0.U.asTypeOf(io.rx_exh_data_out.bits)	


	
	switch(state){
		is(sIDLE){
			when(exh_data_fifo.io.deq.fire() & ibh_meta_fifo.io.deq.fire()){
				// when(!PKG_JUDGE.HAVE_DATA(ibh_meta_fifo.io.deq.bits.op_code)){
				// 	io.rx_exh_data_out.valid:= 0.U
				// }.otherwise{
					io.rx_exh_data_out.valid:= 1.U
				// }
                io.rx_exh_data_out.bits <> exh_data_fifo.io.deq.bits
				when(PKG_JUDGE.RETH_PKG(ibh_meta_fifo.io.deq.bits.op_code)){
					reth_header_tmp					:= exh_data_fifo.io.deq.bits.data(CONFIG.RETH_HEADER_LEN-1,0).asTypeOf(reth_header_tmp)
					io.ibh_meta_out.bits			:= ibh_meta_fifo.io.deq.bits
					io.ibh_meta_out.bits.vaddr 		:= reth_header_tmp.vaddr
					io.ibh_meta_out.bits.length 	:= reth_header_tmp.length					
					// io.ibh_meta_out.bits.exh_gene(ibh_meta_fifo.io.deq.bits.op_code, ibh_meta_fifo.io.deq.bits.qpn, ibh_meta_fifo.io.deq.bits.psn, ibh_meta_fifo.io.deq.bits.isACK, reth_header_tmp.vaddr, reth_header_tmp.length, 0.U, ibh_meta_fifo.io.deq.bits.udp_length)  
				}.elsewhen(PKG_JUDGE.AETH_PKG(ibh_meta_fifo.io.deq.bits.op_code)){
					aeth_header_tmp					:= exh_data_fifo.io.deq.bits.data(CONFIG.AETH_HEADER_LEN-1,0).asTypeOf(aeth_header_tmp)
					io.ibh_meta_out.bits			:= ibh_meta_fifo.io.deq.bits
					io.ibh_meta_out.bits.credit 	:= aeth_header_tmp.credit
					io.ibh_meta_out.bits.isNAK 		:= aeth_header_tmp.isNAK === 3.U		
					io.ibh_meta_out.bits.is_wr_ack 	:= aeth_header_tmp.iswr_ack.asTypeOf(Bool())		
					// io.ibh_meta_out.bits.exh_gene(ibh_meta_fifo.io.deq.bits.op_code, ibh_meta_fifo.io.deq.bits.qpn, ibh_meta_fifo.io.deq.bits.psn, ibh_meta_fifo.io.deq.bits.isACK, 0.U, 0.U, aeth_header_tmp.credit, ibh_meta_fifo.io.deq.bits.udp_length)  
				}.otherwise{
					io.ibh_meta_out.bits			:= ibh_meta_fifo.io.deq.bits
					// io.ibh_meta_out.bits.exh_gene(ibh_meta_fifo.io.deq.bits.op_code, ibh_meta_fifo.io.deq.bits.qpn, ibh_meta_fifo.io.deq.bits.psn, ibh_meta_fifo.io.deq.bits.isACK, 0.U, 0.U, aeth_header_tmp.credit, ibh_meta_fifo.io.deq.bits.udp_length)  
				}
                io.ibh_meta_out.valid   := 1.U    
                when(exh_data_fifo.io.deq.bits.last =/= 1.U){
                    state               := sPAYLOAD
                }

			}
		}
		is(sPAYLOAD){
            when(exh_data_fifo.io.deq.fire()){
                io.rx_exh_data_out.bits     <> exh_data_fifo.io.deq.bits
                io.rx_exh_data_out.valid    := 1.U
                when(exh_data_fifo.io.deq.bits.last === 1.U){
                    state               := sIDLE
                }                
            }
			

		}		
	}


}