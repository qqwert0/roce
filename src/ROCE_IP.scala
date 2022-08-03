package roce

import common.storage._
import common.axi._
import common._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import roce.util._
import roce.table._
import roce.cmd_ctrl._
import roce.rx_exh._
import roce.rx_ibh._
import roce.rx_udpip._
import roce.tx_exh._
import roce.tx_ibh._
import roce.tx_udpip._



class ROCE_IP() extends Module{
	val io = IO(new Bundle{
        //RDMA CMD
        val s_tx_meta  		    = Flipped(Decoupled(new TX_META()))
        //  RDMA SEND DATA
        val s_send_data         = Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
        val m_recv_data         = (Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
        //RDMA RECV META
        val m_recv_meta         = (Decoupled(new RECV_META()))
        //CQ
        val m_cmpt_meta         = (Decoupled(new CMPT_META()))
        //MEM INTERFACE
        val m_mem_read_cmd      = (Decoupled(new MEM_CMD()))
        val s_mem_read_data	    = Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
        val m_mem_write_cmd     = (Decoupled(new MEM_CMD()))
        val m_mem_write_data	= (Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
        //NETWORKER DATA
        val m_net_tx_data       = (Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
        val s_net_rx_data       = Flipped(Decoupled(new AXIS(CONFIG.DATA_WIDTH)))
        //QP INIT
        val msn_init	        = Flipped(Decoupled(new MSN_INIT()))
        val psn_init            = Flipped(Decoupled(new PSN_INIT()))
        val conn_init           = Flipped(Decoupled(new CONN_REQ()))
        val fc_init             = Flipped(Decoupled(new FC_REQ()))
        val cq_init             = Flipped(Decoupled(new CQ_INIT()))

        val local_ip_address    = Input(UInt(32.W))
        val status_reg          = Output(Vec(16,UInt(32.W)))
	})


    ToZero(io.m_cmpt_meta.valid)
	ToZero(io.m_cmpt_meta.bits)
    // ReporterROCE.report(tx_pkg_route.io.pkg_info.ready, "CONN_TABLE===sIDLE")
	

    ////TX/////////////////////////////
    //reth
    val tx_pkg_route = Module(new TX_MEM_PAYLOAD())
    val reth_lshift = Module(new LSHIFT(16,CONFIG.DATA_WIDTH)) 
    val aeth_lshift = Module(new LSHIFT(4,CONFIG.DATA_WIDTH))
    val tx_append_exh = Module(new TX_ADD_EXH())

    val tx_exh_generate = Module(new TX_EXH_FSM())

    //ibh
    val ibh_lshift = Module(new LSHIFT(12,CONFIG.DATA_WIDTH))
    val tx_add_ibh = Module(new TX_ADD_IBH())

    val tx_ibh_fsm = Module(new TX_IBH_FSM())
    //udpip
    val udp_lshift = Module(new LSHIFT(8,CONFIG.DATA_WIDTH))
    val tx_add_udp = Module(new TX_ADD_UDP())
    val ip_lshift = Module(new LSHIFT(20,CONFIG.DATA_WIDTH))
    val tx_add_ip = Module(new TX_ADD_IP())

    ///////RX///////////////////////////

    val rx_exh_payload = Module(new RX_EXH_PAYLOAD())
    val reth_rshift = Module(new RSHIFT(16,CONFIG.DATA_WIDTH))
    val aeth_rshift = Module(new RSHIFT(4,CONFIG.DATA_WIDTH))
	val rx_mem_payload = Module(new RX_MEM_PAYLOAD())

    val rx_data_buffer = XQueue(new AXIS(CONFIG.DATA_WIDTH),4096)

    val rx_exh_fsm = Module(new RX_EXH_FSM())

    //ibh
    val rx_drop_pkg = Module(new RX_DROP_PKG())
    val rx_exh_process = Module(new RX_EXH_PROCESS())
    val ibh_rshift = Module(new RSHIFT(12,CONFIG.DATA_WIDTH))
    val rx_ibh_process = Module(new RX_IBH_PROCESS())

    val rx_ibh_fsm = Module(new RX_IBH_FSM())
    //udpip
    val udp_rshift = Module(new RSHIFT(8,CONFIG.DATA_WIDTH))
    val rx_udp_process = Module(new RX_UDP_PROCESS()) 
    val ip_rshift = Module(new RSHIFT(20,CONFIG.DATA_WIDTH))
    val rx_ip_process = Module(new RX_IP_PROCESS())
    //////////EVENT_CTRL////////////////////

    val event_merge = Module(new EVENT_MERGE())
    val remote_read_handler = Module(new HANDLE_READ_REQ())
    // val mem_read_cmd_merge = Module(new MEM_CMD_MERGER())
    val rdma_cmd_handler = Module(new LOCAL_CMD_HANDLER())
    val credit_judge = Module(new CREDIT_JUDGE())

    ////////////TABLE///////////////////////

    val msn_table = Module(new MSN_TABLE())
    val local_read_vaddr_q = Module(new MULTI_Q(UInt(64.W),512,2048))
    val psn_table = Module(new PSN_TABLE())
    val conn_table = Module(new CONN_TABLE())
    val fc_table = Module(new FC_TABLE())
    val cq_table = Module(new CQ_TABLE())





     /////////////////TX/////////////////////////////////////////

	tx_pkg_route.io.pkg_info  		            <>  event_merge.io.pkg_info      
	tx_pkg_route.io.s_mem_read_data	            <>  io.s_mem_read_data
    tx_pkg_route.io.s_send_data                 <>  io.s_send_data
	reth_lshift.io.in	                        <>  tx_pkg_route.io.reth_data_out
	aeth_lshift.io.in	                        <>  tx_pkg_route.io.aeth_data_out
	tx_append_exh.io.raw_data_in	            <>  tx_pkg_route.io.raw_data_out

	tx_append_exh.io.pkg_info  		            <>  tx_exh_generate.io.pkg_type2exh
	tx_append_exh.io.header_data_in             <>  tx_exh_generate.io.head_data_out
	tx_append_exh.io.reth_data_in	            <>  reth_lshift.io.out
	tx_append_exh.io.aeth_data_in	            <>  aeth_lshift.io.out 

    tx_exh_generate.io.event_in                 <>  credit_judge.io.tx_exh_event
    tx_exh_generate.io.msn2tx_rsp               <>  msn_table.io.msn2tx_rsp 

    //ibh
    ibh_lshift.io.in                            <>  tx_append_exh.io.tx_data_out
    tx_add_ibh.io.ibh_header_in                 <>  tx_ibh_fsm.io.head_data_out  
    tx_add_ibh.io.exh_data_in	                <>  ibh_lshift.io.out

	tx_ibh_fsm.io.ibh_meta_in                   <>  tx_exh_generate.io.ibh_meta_out        
    tx_ibh_fsm.io.psn2tx_rsp                    <>  psn_table.io.psn2tx_rsp
    tx_ibh_fsm.io.conn2tx_rsp                   <>  conn_table.io.conn2tx_rsp 

    //udpip

    udp_lshift.io.in                            <>  tx_add_ibh.io.tx_data_out
	tx_add_udp.io.udpip_meta_in                 <>  tx_ibh_fsm.io.udpip_meta_out 
	tx_add_udp.io.tx_data_in                    <>  udp_lshift.io.out	   

    ip_lshift.io.in                             <>  tx_add_udp.io.tx_data_out
	tx_add_ip.io.ip_meta_in  	                <>  tx_add_udp.io.ip_meta_out
	tx_add_ip.io.tx_data_in                     <>  ip_lshift.io.out	    
    io.m_net_tx_data                            <>  tx_add_ip.io.tx_data_out	    
	tx_add_ip.io.local_ip_address               <>  io.local_ip_address
    /////////////////RX/////////////////////////////////////////
    
    //udpip
    rx_ip_process.io.ip_addr                    <>  io.local_ip_address
    rx_ip_process.io.rx_data_in                 <>  io.s_net_rx_data         		        	        
    ip_rshift.io.in                             <>  rx_ip_process.io.rx_data_out
	rx_udp_process.io.rx_data_in                <>  ip_rshift.io.out         
    rx_udp_process.io.ip_meta_in                <>  rx_ip_process.io.ip_meta_out		          
	udp_rshift.io.in                            <>  rx_udp_process.io.rx_data_out	            
    //ibh
	rx_ibh_process.io.rx_ibh_data_in            <>  udp_rshift.io.out
    rx_ibh_process.io.udpip_meta_in             <>  rx_udp_process.io.udpip_meta_out
    ibh_rshift.io.in                            <>  rx_ibh_process.io.rx_ibh_data_out

	rx_exh_process.io.rx_exh_data_in            <>  ibh_rshift.io.out
    rx_exh_process.io.ibh_meta_in               <>  rx_ibh_process.io.ibh_meta_out
		    		    
	rx_ibh_fsm.io.ibh_meta_in                   <>  rx_exh_process.io.ibh_meta_out
    rx_ibh_fsm.io.psn2rx_rsp                    <>  psn_table.io.psn2rx_rsp

    rx_drop_pkg.io.drop_info  		            <>  rx_ibh_fsm.io.drop_info_out
    rx_drop_pkg.io.rx_meta_in		            <>  rx_ibh_fsm.io.ibh_meta_out
    rx_drop_pkg.io.rx_data_in		            <>  rx_exh_process.io.rx_exh_data_out
    //reth    
    rx_exh_payload.io.pkg_info  	            <>  rx_exh_fsm.io.pkg_type2exh	
    rx_exh_payload.io.rx_ibh_data_in            <>  rx_drop_pkg.io.rx_data_out

    reth_rshift.io.in                           <>  rx_exh_payload.io.reth_data_out
    aeth_rshift.io.in                           <>  rx_exh_payload.io.aeth_data_out


	rx_mem_payload.io.pkg_info  		        <>  rx_exh_fsm.io.pkg_type2mem
	rx_mem_payload.io.reth_data_in	            <>  reth_rshift.io.out
	rx_mem_payload.io.aeth_data_in              <>  aeth_rshift.io.out
	rx_mem_payload.io.raw_data_in	            <>  rx_exh_payload.io.raw_data_out
    rx_data_buffer.io.in                        <>  rx_mem_payload.io.m_mem_write_data
    io.m_recv_data                              <>  rx_mem_payload.io.m_recv_data
    io.m_mem_write_data	                        <>  rx_data_buffer.io.out

	rx_exh_fsm.io.ibh_meta_in                   <>  rx_drop_pkg.io.rx_meta_out
    rx_exh_fsm.io.msn2rx_rsp                    <>  msn_table.io.msn2rx_rsp
    rx_exh_fsm.io.l_read_req_pop_rsp            <>  local_read_vaddr_q.io.pop_rsp
	rx_exh_fsm.io.m_mem_write_cmd               <>  io.m_mem_write_cmd
    rx_exh_fsm.io.m_recv_meta                   <>  io.m_recv_meta


  	// class ila_udp_data(seq:Seq[Data]) extends BaseILA(seq)
    // val udp_data = Wire(UInt(32.W))
    // udp_data    := rx_udp_process.io.rx_data_in.bits.data(31,0)
  	// val mod_udp_data = Module(new ila_udp_data(Seq(	
	// 	rx_udp_process.io.rx_data_in.valid,
	//   	rx_udp_process.io.rx_data_in.ready,
    // 	udp_data
  	// )))
  	// mod_udp_data.connect(clock)

  	// class ila_exh_data(seq:Seq[Data]) extends BaseILA(seq)
    // val exh_data = Wire(UInt(32.W))
    // exh_data    := rx_exh_process.io.rx_exh_data_in.bits.data(31,0)
  	// val mod_exh_data = Module(new ila_exh_data(Seq(	
	// 	rx_exh_process.io.rx_exh_data_in.valid,
	//   	rx_exh_process.io.rx_exh_data_in.ready,
    // 	exh_data
  	// )))
  	// mod_exh_data.connect(clock)

  	// class ila_reth_data(seq:Seq[Data]) extends BaseILA(seq)
    // val reth_data = Wire(UInt(32.W))
    // reth_data    := rx_exh_payload.io.reth_data_out.bits.data(31,0)
  	// val mod_reth_data = Module(new ila_reth_data(Seq(	
	// 	rx_exh_payload.io.reth_data_out.valid,
	//   	rx_exh_payload.io.reth_data_out.ready,
    // 	reth_data
  	// )))
  	// mod_reth_data.connect(clock)

  	// class ila_aeth_data(seq:Seq[Data]) extends BaseILA(seq)
    // val aeth_data = Wire(UInt(32.W))
    // aeth_data    := rx_exh_payload.io.aeth_data_out.bits.data(31,0)
  	// val mod_aeth_data = Module(new ila_aeth_data(Seq(	
	// 	rx_exh_payload.io.aeth_data_out.valid,
	//   	rx_exh_payload.io.aeth_data_out.ready,
    // 	aeth_data
  	// )))
  	// mod_aeth_data.connect(clock)


  	// class ila_arbiter_data(seq:Seq[Data]) extends BaseILA(seq)
    // val arbiter_data = Wire(UInt(32.W))
    // arbiter_data    := rx_mem_payload.io.m_mem_write_data.bits.data(31,0)
  	// val mod_arbiter_data = Module(new ila_arbiter_data(Seq(	
	// 	rx_mem_payload.io.m_mem_write_data.valid,
	//   	rx_mem_payload.io.m_mem_write_data.ready,
    // 	arbiter_data
  	// )))
  	// mod_arbiter_data.connect(clock)      


    ///////////////////////////EVENT CTRL///////////////////////    

    rdma_cmd_handler.io.s_tx_meta  		        <>  io.s_tx_meta
	
	io.m_mem_read_cmd                           <>	event_merge.io.m_mem_read_cmd

	remote_read_handler.io.remote_read_req      <>  rx_exh_fsm.io.r_read_req_req

	event_merge.io.rx_ack_event                 <>  rx_exh_fsm.io.ack_event
	event_merge.io.remote_read_event            <>	remote_read_handler.io.remote_read_event
	event_merge.io.tx_local_event	            <>  rdma_cmd_handler.io.tx_local_event  
    event_merge.io.credit_ack_event             <>  fc_table.io.ack_event

	credit_judge.io.exh_event                   <>  event_merge.io.tx_exh_event         
    credit_judge.io.fc2tx_rsp                   <>  fc_table.io.fc2tx_rsp


       
		
		
    /////////////////////   TABLE   ////////////////////////

	msn_table.io.rx2msn_req                     <>  rx_exh_fsm.io.rx2msn_req
	msn_table.io.tx2msn_req	                    <>  tx_exh_generate.io.tx2msn_req
	msn_table.io.msn_init	                    <>  io.msn_init
		
	local_read_vaddr_q.io.push                  <>  rdma_cmd_handler.io.local_read_addr
	local_read_vaddr_q.io.pop_req               <>  rx_exh_fsm.io.l_read_req_pop_req

	psn_table.io.rx2psn_req                     <>  rx_ibh_fsm.io.rx2psn_req	
	psn_table.io.tx2psn_req	                    <>  tx_ibh_fsm.io.tx2psn_req
	psn_table.io.psn_init	                    <>  io.psn_init
		   
	fc_table.io.rx2fc_req                       <>  rx_exh_fsm.io.rx2fc_req	
	fc_table.io.tx2fc_req	                    <>  credit_judge.io.tx2fc_req
    fc_table.io.buffer_cnt	                    <>  rx_data_buffer.io.count
    fc_table.io.fc_init                         <>  io.fc_init		  	

    conn_table.io.tx2conn_req	                <>  tx_ibh_fsm.io.tx2conn_req
	conn_table.io.conn_init	                    <>  io.conn_init

    cq_table.io.dir_wq_req                      <>  tx_add_udp.io.dir_wq_req
	cq_table.io.rq_req                          <>  rx_exh_fsm.io.rq_req
    cq_table.io.cq_init_req                     <>  io.cq_init
	cq_table.io.cmpt_meta                       <>  io.m_cmpt_meta

    ToZero(io.status_reg)


    val reports = Reg(Vec(ReporterROCE.MAX_NUM,Bool()))
    ToZero(reports)
    ReporterROCE.get_reports(reports)

    ReporterROCE.print_msgs() 

	io.status_reg(0)	:= reports.asUInt()(31,0)
	io.status_reg(1)	:= reports.asUInt()(63,32)
    io.status_reg(2)	:= reports.asUInt()(95,64)
    io.status_reg(3)	:= reports.asUInt()(127,96)

    io.status_reg(4)    := fc_table.io.status_reg(0)
    io.status_reg(5)    := fc_table.io.status_reg(1)

}