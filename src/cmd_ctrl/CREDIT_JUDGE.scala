package roce.cmd_ctrl

import common.storage._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import roce._
import java.text.Collator
import common.Collector

class CREDIT_JUDGE() extends Module{
	val io = IO(new Bundle{
		val exh_event           = Flipped(Decoupled(new IBH_META()))
        val fc2tx_rsp           = Flipped(Decoupled(new FC_RSP()))

        val tx2fc_req           = (Decoupled(new FC_REQ()))
        val tx2fc_ack           = (Decoupled(new FC_REQ()))
        val tx_exh_event	    = (Decoupled(new IBH_META()))
	})

    val exh_event_fifo = Module(new Queue(new IBH_META(), entries=16))
    val fc2tx_rsp_fifo = Module(new Queue(new FC_RSP(), entries=16))

    io.exh_event                       <> exh_event_fifo.io.enq
    io.fc2tx_rsp                       <> fc2tx_rsp_fifo.io.enq
  

	val event_meta = RegInit(0.U.asTypeOf(new IBH_META()))

    val tmp_credit = WireInit(0.U(16.W))

    

	val sIDLE :: sGENERATE :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)	
	Collector.report(state===sIDLE, "CREDIT_JUDGE===sIDLE") 
	exh_event_fifo.io.deq.ready          := (state === sIDLE) & io.tx2fc_req.ready & io.tx2fc_ack.ready 
    fc2tx_rsp_fifo.io.deq.ready          := (state === sGENERATE) & io.tx_exh_event.ready

    io.tx2fc_req.bits 	        := 0.U.asTypeOf(io.tx2fc_req.bits)
    io.tx2fc_req.valid 	        := 0.U
    io.tx2fc_ack.bits 	        := 0.U.asTypeOf(io.tx2fc_ack.bits)
    io.tx2fc_ack.valid 	        := 0.U
    io.tx_exh_event.bits 	    := 0.U.asTypeOf(io.tx_exh_event.bits)
    io.tx_exh_event.valid 	    := 0.U

	
	switch(state){
		is(sIDLE){
			when(exh_event_fifo.io.deq.fire()){
                event_meta                  := exh_event_fifo.io.deq.bits
                when((exh_event_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_WRITE_FIRST) || (exh_event_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_DIRECT_FIRST)){
                    io.tx2fc_req.valid 	        := 1.U  
                    io.tx2fc_req.bits.fc_req_generate(exh_event_fifo.io.deq.bits.qpn, exh_event_fifo.io.deq.bits.op_code, CONFIG.MTU_WORD.U, 0.U, exh_event_fifo.io.deq.bits.is_wr_ack )       
                }.elsewhen(exh_event_fifo.io.deq.bits.op_code === IB_OP_CODE.RC_ACK){
                    io.tx2fc_ack.valid 	        := 1.U 
                    io.tx2fc_ack.bits.fc_req_generate(exh_event_fifo.io.deq.bits.qpn, exh_event_fifo.io.deq.bits.op_code, exh_event_fifo.io.deq.bits.credit, exh_event_fifo.io.deq.bits.psn, exh_event_fifo.io.deq.bits.is_wr_ack )
                }.otherwise{
                    io.tx2fc_req.valid 	        := 1.U  
                    io.tx2fc_req.bits.fc_req_generate(exh_event_fifo.io.deq.bits.qpn, exh_event_fifo.io.deq.bits.op_code, (exh_event_fifo.io.deq.bits.length>>6.U), 0.U, exh_event_fifo.io.deq.bits.is_wr_ack )       
                }                   
                state                       := sGENERATE
			}
		}
		is(sGENERATE){
			when(fc2tx_rsp_fifo.io.deq.fire()){
                when(fc2tx_rsp_fifo.io.deq.bits.valid_event){
                    io.tx_exh_event.valid   := 1.U
                    io.tx_exh_event.bits    <> event_meta 
                }
                state                       := sIDLE
			}
		}        

	}
}