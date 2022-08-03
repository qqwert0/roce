package roce.table

import common.storage._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import roce._


class PSN_TABLE() extends Module{
	val io = IO(new Bundle{
		val rx2psn_req  	= Flipped(Decoupled(new PSN_RX_REQ()))

		val tx2psn_req	    = Flipped(Decoupled(new PSN_TX_REQ()))
		val psn_init	    = Flipped(Decoupled(new PSN_INIT()))
		val psn2rx_rsp	    = (Decoupled(new PSN_STATE()))
		val psn2tx_rsp	    = (Decoupled(new PSN_STATE()))
	})

    val psn_rx_fifo = Module(new Queue(new PSN_RX_REQ(), entries=16))
    val psn_tx_fifo = Module(new Queue(new PSN_TX_REQ(), entries=16))
    val psn_init_fifo = Module(new Queue(new PSN_INIT(), entries=16))

    io.rx2psn_req                       <> psn_rx_fifo.io.enq
    io.tx2psn_req                       <> psn_tx_fifo.io.enq
    io.psn_init                         <> psn_init_fifo.io.enq

    val psn_table = XRam(new PSN_STATE(), CONFIG.MAX_QPS, latency = 1)	

    val psn_tx_req = Reg(new PSN_TX_REQ())
    val psn_tx_tmp = Reg(new PSN_STATE())
    val psn_rx_req = Reg(new PSN_RX_REQ())
    val psn_rx_tmp = Reg(new PSN_STATE())

	val sIDLE :: sTXRSP :: sRXRSP :: sTXRSP1 :: sRXRSP1 :: Nil = Enum(5)
	val state                   = RegInit(sIDLE)
    ReporterROCE.report(state===sIDLE, "PSN_TABLE===sIDLE")  

    psn_table.io.addr_a                 := 0.U
    psn_table.io.addr_b                 := 0.U
    psn_table.io.wr_en_a                := 0.U
    psn_table.io.data_in_a              := 0.U.asTypeOf(psn_table.io.data_in_a)

    psn_rx_fifo.io.deq.ready                 := state === sIDLE
    psn_tx_fifo.io.deq.ready                 := state === sIDLE & (!psn_rx_fifo.io.deq.valid.asBool) 
    psn_init_fifo.io.deq.ready               := state === sIDLE & (!psn_rx_fifo.io.deq.valid.asBool) & (!psn_tx_fifo.io.deq.valid.asBool)

    io.psn2rx_rsp.valid                 := 0.U
    io.psn2rx_rsp.bits                  := 0.U.asTypeOf(io.psn2rx_rsp.bits)

    io.psn2tx_rsp.valid                 := 0.U
    io.psn2tx_rsp.bits                  := 0.U.asTypeOf(io.psn2tx_rsp.bits)
    
	switch(state){
		is(sIDLE){
            when(psn_rx_fifo.io.deq.fire()){
                // when(psn_rx_fifo.io.deq.bits.write){
                //     psn_table.io.addr_a             := psn_rx_fifo.io.deq.bits.qpn
                //     psn_table.io.wr_en_a            := 1.U
                //     psn_table.io.data_in_a.rx_epsn  := psn_rx_fifo.io.deq.bits.rx_epsn
                //     psn_table.io.data_in_a.tx_npsn  := psn_rx_fifo.io.deq.bits.tx_npsn
                //     psn_table.io.data_in_a.tx_old_unack  := psn_rx_fifo.io.deq.bits.tx_old_unack
                //     state                           := sIDLE
                // }.otherwise{
                    psn_table.io.addr_b             := psn_rx_fifo.io.deq.bits.qpn
                    psn_rx_req                      := psn_rx_fifo.io.deq.bits
                    state                           := sRXRSP
                // }
            }.elsewhen(psn_tx_fifo.io.deq.fire()){
                // when(psn_tx_fifo.io.deq.bits.write){
                //     psn_table.io.addr_a             := psn_tx_fifo.io.deq.bits.qpn
                //     psn_table.io.wr_en_a            := 1.U
                //     psn_table.io.data_in_a.rx_epsn  := psn_tx_fifo.io.deq.bits.rx_epsn
                //     psn_table.io.data_in_a.tx_npsn  := psn_tx_fifo.io.deq.bits.tx_npsn
                //     psn_table.io.data_in_a.tx_old_unack  := psn_tx_fifo.io.deq.bits.tx_old_unack
                //     state                           := sIDLE
                // }.otherwise{
                    psn_table.io.addr_b             := psn_tx_fifo.io.deq.bits.qpn
                    psn_tx_req                      := psn_tx_fifo.io.deq.bits
                    state                           := sTXRSP                    
                // }

            }.elsewhen(psn_init_fifo.io.deq.fire()){
                psn_table.io.addr_a             := psn_init_fifo.io.deq.bits.qpn
                psn_table.io.wr_en_a            := 1.U
                psn_table.io.data_in_a.rsp_psn  := 0.U
                psn_table.io.data_in_a.rx_epsn  := psn_init_fifo.io.deq.bits.remote_psn
                // psn_table.io.data_in_a.rx_bitmap  := 0.U
                psn_table.io.data_in_a.tx_npsn  := psn_init_fifo.io.deq.bits.local_psn
                psn_table.io.data_in_a.tx_old_unack  := psn_init_fifo.io.deq.bits.local_psn
                // psn_table.io.data_in_a.tx_bitmap  := 0.U
                state                           := sIDLE
            }.otherwise{
                state                           := sIDLE
            }
		}
		is(sTXRSP){
			when(io.psn2tx_rsp.ready){
				io.psn2tx_rsp.valid 		    := 1.U 
				io.psn2tx_rsp.bits 		        <> psn_table.io.data_out_b
                
                psn_table.io.addr_a             := psn_tx_req.qpn
                psn_table.io.wr_en_a            := 1.U
                psn_table.io.data_in_a.rx_epsn  := psn_table.io.data_out_b.rx_epsn
                when(psn_tx_req.npsn_add === 0.U){
                    psn_table.io.data_in_a.tx_npsn  := psn_table.io.data_out_b.tx_npsn
                    psn_table.io.data_in_a.rsp_psn  := psn_tx_req.rsp_psn
                }.otherwise{
                    psn_table.io.data_in_a.tx_npsn  := psn_table.io.data_out_b.tx_npsn + psn_tx_req.npsn_add
                    psn_table.io.data_in_a.rsp_psn  := psn_table.io.data_out_b.rsp_psn
                }
                
                psn_table.io.data_in_a.tx_old_unack  := psn_table.io.data_out_b.tx_old_unack                
                state                           := sIDLE
			}.otherwise{
                psn_tx_tmp                      := psn_table.io.data_out_b
                state                           := sTXRSP1
            }
		}
		is(sTXRSP1){
			when(io.psn2tx_rsp.ready){
				io.psn2tx_rsp.valid 		    := 1.U 
				io.psn2tx_rsp.bits 		        <> psn_tx_tmp
                
                psn_table.io.addr_a             := psn_tx_req.qpn
                psn_table.io.wr_en_a            := 1.U
                psn_table.io.data_in_a.rx_epsn  := psn_tx_tmp.rx_epsn
                when(psn_tx_req.npsn_add === 0.U){
                    psn_table.io.data_in_a.tx_npsn  := psn_tx_tmp.tx_npsn
                    psn_table.io.data_in_a.rsp_psn  := psn_tx_req.rsp_psn
                }.otherwise{
                    psn_table.io.data_in_a.tx_npsn  := psn_tx_tmp.tx_npsn + psn_tx_req.npsn_add
                    psn_table.io.data_in_a.rsp_psn  := psn_tx_tmp.rsp_psn
                }                
                psn_table.io.data_in_a.tx_old_unack  := psn_tx_tmp.tx_old_unack                
                state                           := sIDLE
			}.otherwise{
                state                           := sTXRSP1
            }
		}        
		is(sRXRSP){
			when(io.psn2rx_rsp.ready){
				io.psn2rx_rsp.valid 		    := 1.U 
				io.psn2rx_rsp.bits 		        <> psn_table.io.data_out_b

                psn_table.io.addr_a             := psn_rx_req.qpn
                psn_table.io.wr_en_a            := 1.U
                psn_table.io.data_in_a.tx_npsn  := psn_table.io.data_out_b.tx_npsn
                psn_table.io.data_in_a.rsp_psn  := psn_table.io.data_out_b.rsp_psn
                when(psn_rx_req.op_code === IB_OP_CODE.RC_ACK){
                    when(psn_rx_req.is_nak){
                        psn_table.io.data_in_a.rx_epsn  := psn_table.io.data_out_b.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack  := psn_table.io.data_out_b.tx_old_unack
                    }.otherwise{
                        psn_table.io.data_in_a.rx_epsn  := psn_table.io.data_out_b.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack  := psn_rx_req.rx_psn                         
                    }                               
                }.elsewhen(PKG_JUDGE.REQ_PKG(psn_rx_req.op_code)){
                    when(psn_rx_req.rx_psn === psn_table.io.data_out_b.rx_epsn){
                        psn_table.io.data_in_a.rx_epsn      := psn_table.io.data_out_b.rx_epsn + 1.U       
                        psn_table.io.data_in_a.tx_old_unack := psn_table.io.data_out_b.tx_old_unack                                
                    }.otherwise{
                        psn_table.io.data_in_a.rx_epsn      := psn_table.io.data_out_b.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack := psn_table.io.data_out_b.tx_old_unack
                    }                              
                }.otherwise{
                    when(psn_rx_req.rx_psn === psn_table.io.data_out_b.tx_old_unack){
                        psn_table.io.data_in_a.rx_epsn      := psn_table.io.data_out_b.rx_epsn       
                        psn_table.io.data_in_a.tx_old_unack := psn_table.io.data_out_b.tx_old_unack + 1.U
                    }.otherwise{
                        psn_table.io.data_in_a.rx_epsn      := psn_table.io.data_out_b.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack := psn_table.io.data_out_b.tx_old_unack                        
                    }
                }
                state                           := sIDLE
			}.otherwise{
                psn_rx_tmp                      := psn_table.io.data_out_b
                state                           := sRXRSP1
            }			
		}	
		is(sRXRSP1){
			when(io.psn2rx_rsp.ready){
				io.psn2rx_rsp.valid 		    := 1.U 
				io.psn2rx_rsp.bits 		        <> psn_rx_tmp

                psn_table.io.addr_a             := psn_rx_req.qpn
                psn_table.io.wr_en_a            := 1.U
                psn_table.io.data_in_a.tx_npsn  := psn_rx_tmp.tx_npsn
                psn_table.io.data_in_a.rsp_psn  := psn_rx_tmp.rsp_psn
                when(psn_rx_req.op_code === IB_OP_CODE.RC_ACK){
                    when(psn_rx_req.is_nak){
                        psn_table.io.data_in_a.rx_epsn  := psn_rx_tmp.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack  := psn_rx_tmp.tx_old_unack
                    }.otherwise{
                        psn_table.io.data_in_a.rx_epsn  := psn_rx_tmp.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack  := psn_rx_req.rx_psn                         
                    }                               
                }.elsewhen(PKG_JUDGE.REQ_PKG(psn_rx_req.op_code)){
                    when(psn_rx_req.rx_psn === psn_rx_tmp.rx_epsn){
                        psn_table.io.data_in_a.rx_epsn      := psn_rx_tmp.rx_epsn + 1.U       
                        psn_table.io.data_in_a.tx_old_unack := psn_rx_tmp.tx_old_unack                                
                    }.otherwise{
                        psn_table.io.data_in_a.rx_epsn      := psn_rx_tmp.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack := psn_rx_tmp.tx_old_unack
                    }
                }.otherwise{
                    when(psn_rx_req.rx_psn === psn_rx_tmp.tx_old_unack){
                        psn_table.io.data_in_a.rx_epsn      := psn_rx_tmp.rx_epsn       
                        psn_table.io.data_in_a.tx_old_unack := psn_rx_tmp.tx_old_unack + 1.U
                    }.otherwise{
                        psn_table.io.data_in_a.rx_epsn      := psn_rx_tmp.rx_epsn        
                        psn_table.io.data_in_a.tx_old_unack := psn_rx_tmp.tx_old_unack                        
                    }
                }
                state                           := sIDLE
			}.otherwise{
                state                           := sRXRSP1
            }			
		}        
	}

}