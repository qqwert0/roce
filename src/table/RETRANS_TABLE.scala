// package roce.table

// import common.storage._
// import chisel3._
// import chisel3.util._
// import chisel3.experimental.ChiselEnum
// import roce.util._



// class RETRANS_TABLE() extends Module{
// 	val io = IO(new Bundle{
// 		val get_event  	    = Flipped(Decoupled(UInt(15.W)))

// 		val insert_event	= Flipped(Decoupled(new INSERT_RETRANS()))
// 		val get_rsp	        = (Decoupled(new IBH_META()))
// 	})

//     val get_event_fifo = Module(new Queue(UInt(15.W), entries=16))
//     val insert_event_fifo = Module(new Queue(new INSERT_RETRANS(), entries=16))

//     io.get_event                            <> get_event_fifo.io.enq
//     io.insert_event                         <> insert_event_fifo.io.enq

//     val retrans_table = XRam(new IBH_META(), 2048, latency = 1)

//     val psn_request = RegInit(0.U.asTypeOf(new PSN_REQ())) 


// 	val sIDLE :: sGETRSP :: Nil = Enum(3)
// 	val state                   = RegInit(sIDLE)

//     retrans_table.io.addr_a                 := 0.U
//     retrans_table.io.addr_b                 := 0.U
//     retrans_table.io.wr_en_a                := 0.U
//     retrans_table.io.data_in_a              := 0.U.asTypeOf(retrans_table.io.data_in_a)

//     insert_event_fifo.io.deq.ready                 := state === sIDLE
//     get_event_fifo.io.deq.ready                 := state === sIDLE & (!insert_event_fifo.io.deq.valid.asBool) 

//     io.get_rsp.valid                 := 0.U
//     io.get_rsp.bits                  := 0.U.asTypeOf(io.get_rsp.bits)
    
// 	switch(state){
// 		is(sIDLE){
//             when(insert_event_fifo.io.deq.fire()){
//                 retrans_table.io.addr_a             := insert_event_fifo.io.deq.bits.index
//                 retrans_table.io.wr_en_a            := 1.U
//                 retrans_table.io.data_in_a          := insert_event_fifo.io.deq.bits.event
//                 state                               := sIDLE
//             }.elsewhen(get_event_fifo.io.deq.fire()){
//                 retrans_table.io.addr_b             := get_event_fifo.io.deq.bits
//                 state                               := sGETRSP                    

//             }.otherwise{
//                 state                           := sIDLE
//             }
// 		}
// 		is(sGETRSP){
// 			when(io.get_rsp.ready){
// 				io.get_rsp.valid 		        := 1.U 
// 				io.get_rsp.bits 		        <> retrans_table.io.data_out_b
//                 state                           := sIDLE
// 			}.otherwise{
//                 state                           := sGETRSP
//             }
// 		}	
// 	}

// }




