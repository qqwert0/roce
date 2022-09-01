package roce

import common.storage._
import common.Delay
import common.connection._
import common.axi._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import roce._

class IP_LOOP() extends Module{
	val io = IO(new Bundle{

        val s_tx_meta  		    = Vec(2,Flipped(Decoupled(new TX_META())))

        val s_send_data         = Vec(2,Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH))))
        val m_recv_data         = Vec(2,(Decoupled(new AXIS(CONFIG.DATA_WIDTH))))
        val m_recv_meta         = Vec(2,(Decoupled(new RECV_META())))
        val m_cmpt_meta         = Vec(2,(Decoupled(new CMPT_META())))


        val m_mem_read_cmd      = Vec(2,(Decoupled(new MEM_CMD())))
        val s_mem_read_data	    = Vec(2,Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH))))
        val m_mem_write_cmd     = Vec(2,(Decoupled(new MEM_CMD())))
        val m_mem_write_data	= Vec(2,(Decoupled(new AXIS(CONFIG.DATA_WIDTH))))


        val qp_init	            = Vec(2,Flipped(Decoupled(new QP_INIT())))
        val local_ip_address    = Vec(2,Input(UInt(32.W)))

        // val reports             = Output(UInt(32.W))
	})


    val roce = Seq.fill(2)(Module(new ROCE_IP()))

    val q = XQueue(2)(new AXIS(CONFIG.DATA_WIDTH), 16)

    // val delay = Delay(4)(new AXIS(CONFIG.DATA_WIDTH), 1000)




    for(i<- 0 until 2){
        roce(i).io.m_net_tx_data                <>q(i).io.in
        


        io.s_tx_meta(i)                 <> roce(i).io.s_tx_meta  	        
        io.qp_init(i)                   <> roce(i).io.qp_init
        io.local_ip_address(i)          <> roce(i).io.local_ip_address
        io.m_mem_read_cmd(i)            <> roce(i).io.m_mem_read_cmd 
        io.s_mem_read_data(i)           <> roce(i).io.s_mem_read_data 
        io.m_mem_write_cmd(i)           <> roce(i).io.m_mem_write_cmd 
        io.m_mem_write_data(i)          <> roce(i).io.m_mem_write_data    
        io.s_send_data(i)               <> roce(i).io.s_send_data     
        io.m_recv_data(i)               <> roce(i).io.m_recv_data   
        io.m_recv_meta(i)               <> roce(i).io.m_recv_meta   
        io.m_cmpt_meta(i)               <> roce(i).io.m_cmpt_meta   
    } 
    roce(0).io.s_net_rx_data                <>q(1).io.out
    roce(1).io.s_net_rx_data                <>q(0).io.out
}