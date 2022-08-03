package roce.rx_exh

import common.storage._
import common.axi._
import common.ToZero
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._

class RX_MEM_PAYLOAD() extends Module{
	val io = IO(new Bundle{
		val pkg_info  		= Flipped(Decoupled(new RX_PKG_INFO()))		
		val reth_data_in	= Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
		val aeth_data_in	= Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
		val raw_data_in	    = Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH)))

        val m_mem_write_data	= (Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
		val m_recv_data		= (Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
	})




	val length_cnt = RegInit(0.U(16.W))
	val pkg_info = RegInit(0.U.asTypeOf(new RX_PKG_INFO()))
	val sIDLE :: sAETH :: sRETH :: sRAW :: Nil = Enum(4)
	val state          = RegInit(sIDLE)	
	ReporterROCE.report(state===sIDLE, "RX_MEM_PAYLOAD===sIDLE")
	
	io.pkg_info.ready := (state === sIDLE)

	io.reth_data_in.ready               := (state === sRETH) & io.m_mem_write_data.ready
    io.aeth_data_in.ready               := (state === sAETH) & io.m_mem_write_data.ready
    io.raw_data_in.ready                := (state === sRAW) & io.m_mem_write_data.ready
	
	ToZero(io.m_mem_write_data.valid)
	ToZero(io.m_mem_write_data.bits)
	
	ToZero(io.m_recv_data.bits)		
	ToZero(io.m_recv_data.valid)
	
	switch(state){
		is(sIDLE){
			when(io.pkg_info.fire()){
				pkg_info	<> io.pkg_info.bits
				length_cnt	:= 0.U
				when(io.pkg_info.bits.pkg_type === PKG_TYPE.AETH){
					state	:= sAETH
				}.elsewhen(io.pkg_info.bits.pkg_type === PKG_TYPE.RETH){
                    state	:= sRETH
                }.otherwise{
					state	:= sRAW
				}
			}
		}
		is(sAETH){
			when(pkg_info.data_to_mem){
				when(io.aeth_data_in.fire()){
					io.m_mem_write_data.valid 		:= 1.U 
					io.m_mem_write_data.bits 		<> io.aeth_data_in.bits
					when(io.aeth_data_in.bits.last === 1.U){
						state						:= sIDLE
					}.otherwise{
						state						:= sAETH
					}
				}				
			}.otherwise{
				when(io.aeth_data_in.fire()){
					io.m_recv_data.valid 		:= 1.U 
					io.m_recv_data.bits 		<> io.aeth_data_in.bits
					when(io.aeth_data_in.bits.last === 1.U){
						state						:= sIDLE
					}.otherwise{
						state						:= sAETH
					}
				}					
			}

		}
		is(sRETH){
			when(pkg_info.data_to_mem){
				when(io.reth_data_in.fire()){
					io.m_mem_write_data.valid 		:= 1.U 
					io.m_mem_write_data.bits 		<> io.reth_data_in.bits
					when(io.reth_data_in.bits.last === 1.U){
						state						:= sIDLE
					}.otherwise{
						state						:= sRETH
					}
				}	
			}.otherwise{
				when(io.reth_data_in.fire()){
					io.m_recv_data.valid 		:= 1.U 
					io.m_recv_data.bits 		<> io.reth_data_in.bits
					when(io.reth_data_in.bits.last === 1.U){
						state						:= sIDLE
					}.otherwise{
						state						:= sRETH
					}
				}					
			}		
		}
		is(sRAW){
			when(pkg_info.data_to_mem){
				when(io.raw_data_in.fire()){
					io.m_mem_write_data.valid 		:= 1.U 
					io.m_mem_write_data.bits 		<> io.raw_data_in.bits
					when(io.raw_data_in.bits.last === 1.U){
						state						:= sIDLE
					}.otherwise{
						state						:= sRAW
					}
				}	
			}.otherwise{
				when(io.raw_data_in.fire()){
					io.m_recv_data.valid 		:= 1.U 
					io.m_recv_data.bits 		<> io.raw_data_in.bits
					when(io.raw_data_in.bits.last === 1.U){
						state						:= sIDLE
					}.otherwise{
						state						:= sRAW
					}
				}				
			}

		}		
	}


}