`timescale 1ns / 1ps


// module tb_rdma_config_init(

//     );

//     reg[159:0]   rdma_config[0:1023];
//     reg[15:0]   qpn_nums = 1;
//     reg[15:0]   rdma_cmd_nums = 74; 
//     reg[15:0]   mem_block_nums = 4;
//     reg[15:0]   check_mem_nums = rdma_cmd_nums;

//     reg[35:0]   length,offset;
//     reg[47:0]   r_vaddr,l_vaddr;
//     reg[1:0]    rdma_cmd,node_index,dest_index;
    
//     integer i;

//     /*    4 node: node0:A, node1:B, node2:C, node3:D

//     QP1:  A.1  B.3  
//     QP2:  A.2  C.7
//     QP3:  A.3  D.10
//     QP4:  B.4  C.8
//     QP5:  B.5  D.9
//     QP6:  C.6  D.6

//     */


//     initial begin
//         rdma_config[0] = {96'h0,check_mem_nums,mem_block_nums,rdma_cmd_nums,qpn_nums};
//         rdma_config[1] = {32'd0,32'd4,32'h200000,32'd0,32'd0}; //res,offset,length,start_addr,node_idx
//         rdma_config[2] = {32'd0,32'd5,32'h200000,32'd0,32'd1}; //res,offset,length,start_addr,node_idx
//         rdma_config[3] = {32'd0,32'd6,32'h200000,32'd0,32'd2}; //res,offset,length,start_addr,node_idx
//         rdma_config[4] = {32'd0,32'd7,32'h200000,32'd0,32'd3}; //res,offset,length,start_addr,node_idx

//         for(i=0;i<rdma_cmd_nums;i=i+1)begin
//             length = (({$random} % 16384)<<6) + 64;
//             rdma_cmd = ({$random} % 2);
//             node_index = ({$random} % 4);
//             dest_index = ({$random} % 3);
//             if(node_index == dest_index)begin
//                 dest_index = 3;
//             end
//             if(rdma_cmd)begin
//                 l_vaddr = ({$random} % 16384)<< 6 ;
//                 r_vaddr = (({$random} % 16384)<< 6) +32'h200000;
//             end 
//             else begin
//                 r_vaddr = ({$random} % 16384)<< 6 ; 
//                 l_vaddr = (({$random} % 16384)<< 6) +32'h200000;
//             end
//             if(node_index==0)begin
//                 if(dest_index==1)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd1,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+4 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+5 ,l_vaddr,node_index};
//                 end else if(dest_index==2)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd2,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+4 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+6 ,l_vaddr,node_index};                    
//                 end else begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd3,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+4 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+7 ,l_vaddr,node_index};
//                 end
                    
//             end 
//             else if(node_index==1)begin
//                 if(dest_index==0)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd3,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+5 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+4 ,l_vaddr,node_index};
//                 end else if(dest_index==2)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd4,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+5 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+6 ,l_vaddr,node_index};                    
//                 end else begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd5,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+5 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+7 ,l_vaddr,node_index};                    
//                 end
//             end 
//             else if(node_index==2)begin
//                 if(dest_index==0)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd7,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+6 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+4 ,l_vaddr,node_index};
//                 end else if(dest_index==1)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd8,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+6 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+5 ,l_vaddr,node_index};
//                 end else begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd6,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+6 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+7 ,l_vaddr,node_index};
//                 end
//             end
//             else begin
//                 if(dest_index==0)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd10,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+7 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+4 ,l_vaddr,node_index};                    
//                 end else if(dest_index==1)begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd9,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+7 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+5 ,l_vaddr,node_index};
//                 end else begin
//                     rdma_config[i+5] = {length,r_vaddr,l_vaddr,24'd6,rdma_cmd,node_index};
//                     if(rdma_cmd)
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(l_vaddr>>6)+7 ,r_vaddr,dest_index};
//                     else
//                         rdma_config[i+5+rdma_cmd_nums] = {32'b0,length,(r_vaddr>>6)+6 ,l_vaddr,node_index};
//                 end
//             end
//         end
  
//         $writememh("config.hex",rdma_config);
//     end


// endmodule

// module tb_rdma_config_init(

//     );

//     reg[159:0]   rdma_config[0:199];
//     reg[15:0]   qpn_nums = 1;
//     reg[15:0]   rdma_cmd_nums = 74; 
//     reg[15:0]   mem_block_nums = 4;
//     reg[15:0]   check_mem_nums = 74;
//     integer i;

//     /*    4 node: node0:A, node1:B, node2:C, node3:D

//     QP1:  A.1  B.3  
//     QP2:  A.2  C.7
//     QP3:  A.3  D.10
//     QP4:  B.4  C.8
//     QP5:  B.5  D.9
//     QP6:  C.6  D.6

//     */


//     initial begin
//         rdma_config[0] = {96'h0,check_mem_nums,mem_block_nums,rdma_cmd_nums,qpn_nums};
//         rdma_config[1] = {32'd0,32'd4,32'h100000,32'd0,32'd0}; //res,offset,length,start_addr,node_idx
//         rdma_config[2] = {32'd0,32'd5,32'h100000,32'd0,32'd1}; //res,offset,length,start_addr,node_idx
//         rdma_config[3] = {32'd0,32'd6,32'h100000,32'd0,32'd2}; //res,offset,length,start_addr,node_idx
//         rdma_config[4] = {32'd0,32'd7,32'h100000,32'd0,32'd3}; //res,offset,length,start_addr,node_idx

//         rdma_config[5] = {32'h40,48'h40,48'h100000,24'd1,2'd0,2'd0};//length, r_addr, l_vaddr, l_qpn, rdma_cmd: 0:read 1:writre, node_idx 
//         rdma_config[6] = {32'h40,48'h40,48'h100080,24'd1,2'd0,2'd0};
//         rdma_config[7] = {32'h40,48'h40,48'h100100,24'd1,2'd0,2'd0};
//         rdma_config[8] = {32'h40,48'h40,48'h100180,24'd1,2'd0,2'd0};
//         rdma_config[9] = {32'h40,48'h40,48'h100200,24'd1,2'd0,2'd0};
//         rdma_config[10] = {32'h40,48'h40,48'h100280,24'd1,2'd0,2'd0};
//         rdma_config[11] = {32'h40,48'h100000,48'h40,24'd1,2'd1,2'd0};
//         rdma_config[12] = {32'h40,48'h100080,48'h40,24'd1,2'd1,2'd0};
//         rdma_config[13] = {32'h40,48'h100100,48'h40,24'd1,2'd1,2'd0};
//         rdma_config[14] = {32'h40,48'h100180,48'h40,24'd1,2'd1,2'd0};
//         rdma_config[15] = {32'h40,48'h100200,48'h40,24'd1,2'd1,2'd0};
//         rdma_config[16] = {32'h40,48'h100280,48'h40,24'd1,2'd1,2'd0};

//         rdma_config[17] = {32'h400,48'h80,48'h100800,24'd2,2'd0,2'd0};
//         rdma_config[18] = {32'h400,48'h80,48'h101000,24'd2,2'd0,2'd0};
//         rdma_config[19] = {32'h400,48'h80,48'h101800,24'd2,2'd0,2'd0};
//         rdma_config[20] = {32'h400,48'h80,48'h102000,24'd2,2'd0,2'd0};
//         rdma_config[21] = {32'h400,48'h80,48'h102800,24'd2,2'd0,2'd0};
//         rdma_config[22] = {32'h400,48'h100800,48'h100,24'd2,2'd1,2'd0};
//         rdma_config[23] = {32'h400,48'h101000,48'h100,24'd2,2'd1,2'd0};
//         rdma_config[24] = {32'h400,48'h101800,48'h100,24'd2,2'd1,2'd0};
//         rdma_config[25] = {32'h400,48'h102000,48'h100,24'd2,2'd1,2'd0};
//         rdma_config[26] = {32'h400,48'h102800,48'h100,24'd2,2'd1,2'd0};

//         rdma_config[27] = {32'h4000,48'h40,48'h108000,24'd3,2'd0,2'd0};
//         rdma_config[28] = {32'h4000,48'h40,48'h110000,24'd3,2'd0,2'd0};
//         rdma_config[29] = {32'h4000,48'h40,48'h118000,24'd3,2'd0,2'd0};
//         rdma_config[30] = {32'h4000,48'h40,48'h120000,24'd3,2'd0,2'd0};
//         rdma_config[31] = {32'h4000,48'h40,48'h128000,24'd3,2'd0,2'd0};
//         rdma_config[32] = {32'h4000,48'h108000,48'h40,24'd3,2'd1,2'd0};
//         rdma_config[33] = {32'h4000,48'h110000,48'h40,24'd3,2'd1,2'd0};
//         rdma_config[34] = {32'h4000,48'h118000,48'h40,24'd3,2'd1,2'd0};
//         rdma_config[35] = {32'h4000,48'h120000,48'h40,24'd3,2'd1,2'd0};
//         rdma_config[36] = {32'h4000,48'h128000,48'h40,24'd3,2'd1,2'd0};

//         rdma_config[37] = {32'h40,48'h40,48'h200000,24'd3,2'd0,2'd1};//length, r_addr, l_vaddr, l_qpn, rdma_cmd: 0:read 1:writre, node_idx 
//         rdma_config[38] = {32'h40,48'h40,48'h200080,24'd3,2'd0,2'd1};
//         rdma_config[39] = {32'h40,48'h40,48'h200100,24'd3,2'd0,2'd1};
//         rdma_config[40] = {32'h40,48'h40,48'h200180,24'd3,2'd0,2'd1};
//         rdma_config[41] = {32'h40,48'h40,48'h200200,24'd3,2'd0,2'd1};
//         rdma_config[42] = {32'h40,48'h40,48'h200280,24'd3,2'd0,2'd1};
//         rdma_config[43] = {32'h40,48'h200000,48'h40,24'd3,2'd1,2'd1};
//         rdma_config[44] = {32'h40,48'h200080,48'h40,24'd3,2'd1,2'd1};
//         rdma_config[45] = {32'h40,48'h200100,48'h40,24'd3,2'd1,2'd1};
//         rdma_config[46] = {32'h40,48'h200180,48'h40,24'd3,2'd1,2'd1};
//         rdma_config[47] = {32'h40,48'h200200,48'h40,24'd3,2'd1,2'd1};
//         rdma_config[48] = {32'h40,48'h200280,48'h40,24'd3,2'd1,2'd1};

//         rdma_config[49] = {32'h400,48'h80,48'h200800,24'd4,2'd0,2'd1};
//         rdma_config[50] = {32'h400,48'h80,48'h201000,24'd4,2'd0,2'd1};
//         rdma_config[51] = {32'h400,48'h80,48'h201800,24'd4,2'd0,2'd1};
//         rdma_config[52] = {32'h400,48'h80,48'h202000,24'd4,2'd0,2'd1};
//         rdma_config[53] = {32'h400,48'h80,48'h202800,24'd4,2'd0,2'd1};
//         rdma_config[54] = {32'h400,48'h200800,48'h100,24'd4,2'd1,2'd1};
//         rdma_config[55] = {32'h400,48'h201000,48'h100,24'd4,2'd1,2'd1};
//         rdma_config[56] = {32'h400,48'h201800,48'h100,24'd4,2'd1,2'd1};
//         rdma_config[57] = {32'h400,48'h202000,48'h100,24'd4,2'd1,2'd1};
//         rdma_config[58] = {32'h400,48'h202800,48'h100,24'd4,2'd1,2'd1};
//         rdma_config[59] = {32'h4000,48'h40,48'h208000,24'd5,2'd0,2'd1};
//         rdma_config[60] = {32'h4000,48'h40,48'h210000,24'd5,2'd0,2'd1};
//         rdma_config[61] = {32'h4000,48'h40,48'h218000,24'd5,2'd0,2'd1};
//         rdma_config[62] = {32'h4000,48'h40,48'h220000,24'd5,2'd0,2'd1};
//         rdma_config[63] = {32'h4000,48'h40,48'h228000,24'd5,2'd0,2'd1};
//         rdma_config[64] = {32'h4000,48'h208000,48'h40,24'd5,2'd1,2'd1};
//         rdma_config[65] = {32'h4000,48'h210000,48'h40,24'd5,2'd1,2'd1};
//         rdma_config[66] = {32'h4000,48'h218000,48'h40,24'd5,2'd1,2'd1};
//         rdma_config[67] = {32'h4000,48'h220000,48'h40,24'd5,2'd1,2'd1};
//         rdma_config[68] = {32'h4000,48'h228000,48'h40,24'd5,2'd1,2'd1};           
//         rdma_config[69] = {32'h4000,48'h40,48'h248000,24'd6,2'd0,2'd2};
//         rdma_config[70] = {32'h4000,48'h40,48'h250000,24'd6,2'd0,2'd2};
//         rdma_config[71] = {32'h4000,48'h40,48'h258000,24'd6,2'd0,2'd2};
//         rdma_config[72] = {32'h4000,48'h40,48'h260000,24'd6,2'd0,2'd2};
//         rdma_config[73] = {32'h4000,48'h40,48'h268000,24'd6,2'd0,2'd2};
//         rdma_config[74] = {32'h4000,48'h248000,48'h40,24'd6,2'd1,2'd2};
//         rdma_config[75] = {32'h4000,48'h250000,48'h40,24'd6,2'd1,2'd2};
//         rdma_config[76] = {32'h4000,48'h258000,48'h40,24'd6,2'd1,2'd2};
//         rdma_config[77] = {32'h4000,48'h260000,48'h40,24'd6,2'd1,2'd2};
//         rdma_config[78] = {32'h4000,48'h268000,48'h40,24'd6,2'd1,2'd2};

//         rdma_config[79] = {32'd0,32'd6,32'h40,32'h100000,32'd0};//res,offset,length,start_addr,node_idx
//         rdma_config[80] = {32'd0,32'd6,32'h40,32'h100080,32'd0};
//         rdma_config[81] = {32'd0,32'd6,32'h40,32'h100100,32'd0};
//         rdma_config[82] = {32'd0,32'd6,32'h40,32'h100180,32'd0};
//         rdma_config[83] = {32'd0,32'd6,32'h40,32'h100200,32'd0};
//         rdma_config[84] = {32'd0,32'd6,32'h40,32'h100280,32'd0};
//         rdma_config[85] = {32'd0,32'd5,32'h40,32'h100000,32'd1};//res,offset,length,start_addr,node_idx
//         rdma_config[86] = {32'd0,32'd5,32'h40,32'h100080,32'd1};
//         rdma_config[87] = {32'd0,32'd5,32'h40,32'h100100,32'd1};
//         rdma_config[88] = {32'd0,32'd5,32'h40,32'h100180,32'd1};
//         rdma_config[89] = {32'd0,32'd5,32'h40,32'h100200,32'd1};
//         rdma_config[90] = {32'd0,32'd5,32'h40,32'h100280,32'd1};
//         rdma_config[91] = {32'd0,32'd8,32'h400,32'h100800,32'd0};
//         rdma_config[92] = {32'd0,32'd8,32'h400,32'h101000,32'd0};
//         rdma_config[93] = {32'd0,32'd8,32'h400,32'h101800,32'd0};
//         rdma_config[94] = {32'd0,32'd8,32'h400,32'h102000,32'd0};
//         rdma_config[95] = {32'd0,32'd8,32'h400,32'h102800,32'd0};
//         rdma_config[96] = {32'd0,32'd8,32'h400,32'h100800,32'd2};
//         rdma_config[97] = {32'd0,32'd8,32'h400,32'h101000,32'd2};
//         rdma_config[98] = {32'd0,32'd8,32'h400,32'h101800,32'd2};
//         rdma_config[99] = {32'd0,32'd8,32'h400,32'h102000,32'd2};
//         rdma_config[100] = {32'd0,32'd8,32'h400,32'h102800,32'd2};
//         rdma_config[101] = {32'd0,32'd8,32'h4000,32'h108000,32'd0};
//         rdma_config[102] = {32'd0,32'd8,32'h4000,32'h110000,32'd0};
//         rdma_config[103] = {32'd0,32'd8,32'h4000,32'h118000,32'd0};
//         rdma_config[104] = {32'd0,32'd8,32'h4000,32'h120000,32'd0};
//         rdma_config[105] = {32'd0,32'd8,32'h4000,32'h128000,32'd0};
//         rdma_config[106] = {32'd0,32'd5,32'h4000,32'h108000,32'd3};
//         rdma_config[107] = {32'd0,32'd5,32'h4000,32'h110000,32'd3};
//         rdma_config[108] = {32'd0,32'd5,32'h4000,32'h118000,32'd3};
//         rdma_config[109] = {32'd0,32'd5,32'h4000,32'h120000,32'd3};
//         rdma_config[110] = {32'd0,32'd5,32'h4000,32'h128000,32'd3};
//         rdma_config[111] = {32'd0,32'd5,32'h40,32'h200000,32'd1};//res,offset,length,start_addr,node_idx
//         rdma_config[112] = {32'd0,32'd5,32'h40,32'h200080,32'd1};
//         rdma_config[113] = {32'd0,32'd5,32'h40,32'h200100,32'd1};
//         rdma_config[114] = {32'd0,32'd5,32'h40,32'h200180,32'd1};
//         rdma_config[115] = {32'd0,32'd5,32'h40,32'h200200,32'd1};
//         rdma_config[116] = {32'd0,32'd5,32'h40,32'h200280,32'd1};
//         rdma_config[117] = {32'd0,32'd6,32'h40,32'h200000,32'd0};//res,offset,length,start_addr,node_idx
//         rdma_config[118] = {32'd0,32'd6,32'h40,32'h200080,32'd0};
//         rdma_config[119] = {32'd0,32'd6,32'h40,32'h200100,32'd0};
//         rdma_config[120] = {32'd0,32'd6,32'h40,32'h200180,32'd0};
//         rdma_config[121] = {32'd0,32'd6,32'h40,32'h200200,32'd0};
//         rdma_config[122] = {32'd0,32'd6,32'h40,32'h200280,32'd0};
//         rdma_config[123] = {32'd0,32'd8,32'h400,32'h200800,32'd1};
//         rdma_config[124] = {32'd0,32'd8,32'h400,32'h201000,32'd1};
//         rdma_config[125] = {32'd0,32'd8,32'h400,32'h201800,32'd1};
//         rdma_config[126] = {32'd0,32'd8,32'h400,32'h202000,32'd1};
//         rdma_config[127] = {32'd0,32'd8,32'h400,32'h202800,32'd1};
//         rdma_config[128] = {32'd0,32'd9,32'h400,32'h200800,32'd2};
//         rdma_config[129] = {32'd0,32'd9,32'h400,32'h201000,32'd2};
//         rdma_config[130] = {32'd0,32'd9,32'h400,32'h201800,32'd2};
//         rdma_config[131] = {32'd0,32'd9,32'h400,32'h202000,32'd2};
//         rdma_config[132] = {32'd0,32'd9,32'h400,32'h202800,32'd2};
//         rdma_config[133] = {32'd0,32'd8,32'h4000,32'h208000,32'd1};
//         rdma_config[134] = {32'd0,32'd8,32'h4000,32'h210000,32'd1};
//         rdma_config[135] = {32'd0,32'd8,32'h4000,32'h218000,32'd1};
//         rdma_config[136] = {32'd0,32'd8,32'h4000,32'h220000,32'd1};
//         rdma_config[137] = {32'd0,32'd8,32'h4000,32'h228000,32'd1};
//         rdma_config[138] = {32'd0,32'd6,32'h4000,32'h208000,32'd3};
//         rdma_config[139] = {32'd0,32'd6,32'h4000,32'h210000,32'd3};
//         rdma_config[140] = {32'd0,32'd6,32'h4000,32'h218000,32'd3};
//         rdma_config[141] = {32'd0,32'd6,32'h4000,32'h220000,32'd3};
//         rdma_config[142] = {32'd0,32'd6,32'h4000,32'h228000,32'd3};        
//         rdma_config[143] = {32'd0,32'd8,32'h4000,32'h248000,32'd2};
//         rdma_config[144] = {32'd0,32'd8,32'h4000,32'h250000,32'd2};
//         rdma_config[145] = {32'd0,32'd8,32'h4000,32'h258000,32'd2};
//         rdma_config[146] = {32'd0,32'd8,32'h4000,32'h260000,32'd2};
//         rdma_config[147] = {32'd0,32'd8,32'h4000,32'h268000,32'd2};
//         rdma_config[148] = {32'd0,32'd7,32'h4000,32'h248000,32'd3};
//         rdma_config[149] = {32'd0,32'd7,32'h4000,32'h250000,32'd3};
//         rdma_config[150] = {32'd0,32'd7,32'h4000,32'h258000,32'd3};
//         rdma_config[151] = {32'd0,32'd7,32'h4000,32'h260000,32'd3};
//         rdma_config[152] = {32'd0,32'd7,32'h4000,32'h268000,32'd3};  

  
//         $writememh("config.hex",rdma_config);
//     end


// endmodule


// module tb_rdma_config_init(

//     );

//     reg[159:0]   rdma_config[0:127];
//     reg[15:0]   qpn_nums = 1;
//     reg[15:0]   rdma_cmd_nums = 22; 
//     reg[15:0]   mem_block_nums = 1;
//     reg[15:0]   check_mem_nums = 22;
//     integer i;

//     initial begin
//         rdma_config[0] = {96'h0,check_mem_nums,mem_block_nums,rdma_cmd_nums,qpn_nums};
//         rdma_config[1] = {32'd0,32'd3,32'd200000,32'd960,32'd1};
//         rdma_config[2] = {32'd64,48'd960,48'd64,24'd1,2'd0,1'd0};
//         rdma_config[3] = {32'd128,48'd960,48'd512,24'd1,2'd0,1'd0};
//         rdma_config[4] = {32'd1344,48'd1024,48'd1024,24'd1,2'd0,1'd0};
//         rdma_config[5] = {32'd1408,48'd1024,48'd8000,24'd1,2'd0,1'd0};
//         rdma_config[6] = {32'd1472,48'd1024,48'd16000,24'd1,2'd0,1'd0};
//         rdma_config[7] = {32'd2752,48'd1024,48'd24000,24'd1,2'd0,1'd0};
//         rdma_config[8] = {32'd2816,48'd1024,48'd32000,24'd1,2'd0,1'd0};
//         rdma_config[9] = {32'd2880,48'd1024,48'd40000,24'd1,2'd0,1'd0};
//         rdma_config[10] = {32'd4096,48'd1024,48'd48000,24'd1,2'd0,1'd0};
//         rdma_config[11] = {32'd4224,48'd1024,48'd56000,24'd1,2'd0,1'd0};
//         rdma_config[12] = {32'd40000,48'd1024,48'd64000,24'd1,2'd0,1'd0};

//         rdma_config[13] = {32'd64,48'd200000,48'd960,24'd1,2'd1,1'd1};
//         rdma_config[14] = {32'd128,48'd200128,48'd960,24'd1,2'd1,1'd1};
//         rdma_config[15] = {32'd1344,48'd201024,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[16] = {32'd1408,48'd208000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[17] = {32'd1472,48'd216000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[18] = {32'd2752,48'd224000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[19] = {32'd2816,48'd232000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[20] = {32'd2880,48'd240000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[21] = {32'd4096,48'd248000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[22] = {32'd4224,48'd256000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[23] = {32'd40000,48'd264000,48'd1024,24'd1,2'd1,1'd1};        

//         rdma_config[24] = {32'd0,32'd3,32'd64,32'd64,32'd0};
//         rdma_config[25] = {32'd0,32'd3,32'd128,32'd512,32'd0};
//         rdma_config[26] = {32'd0,32'd4,32'd1344,32'd1024,32'd0};
//         rdma_config[27] = {32'd0,32'd4,32'd1408,32'd8000,32'd0};
//         rdma_config[28] = {32'd0,32'd4,32'd1472,32'd16000,32'd0};
//         rdma_config[29] = {32'd0,32'd4,32'd2752,32'd24000,32'd0};
//         rdma_config[30] = {32'd0,32'd4,32'd2816,32'd32000,32'd0};
//         rdma_config[31] = {32'd0,32'd4,32'd2880,32'd40000,32'd0};
//         rdma_config[32] = {32'd0,32'd4,32'd4096,32'd48000,32'd0};
//         rdma_config[33] = {32'd0,32'd4,32'd4224,32'd56000,32'd0};
//         rdma_config[34] = {32'd0,32'd4,32'd40000,32'd64000,32'd0};

//         rdma_config[35] = {32'd0,32'd3,32'd64,32'd200000,32'd0};
//         rdma_config[36] = {32'd0,32'd3,32'd128,32'd200128,32'd0};
//         rdma_config[37] = {32'd0,32'd4,32'd1344,32'd201024,32'd0};
//         rdma_config[38] = {32'd0,32'd4,32'd1408,32'd208000,32'd0};
//         rdma_config[39] = {32'd0,32'd4,32'd1472,32'd216000,32'd0};
//         rdma_config[40] = {32'd0,32'd4,32'd2752,32'd224000,32'd0};
//         rdma_config[41] = {32'd0,32'd4,32'd2816,32'd232000,32'd0};
//         rdma_config[42] = {32'd0,32'd4,32'd2880,32'd240000,32'd0};
//         rdma_config[43] = {32'd0,32'd4,32'd4096,32'd248000,32'd0};
//         rdma_config[44] = {32'd0,32'd4,32'd4224,32'd256000,32'd0};
//         rdma_config[45] = {32'd0,32'd4,32'd40000,32'd264000,32'd0};        
//         $writememh("config.hex",rdma_config);
//     end


// endmodule

// module tb_rdma_config_init(

//     );

//     reg[159:0]   rdma_config[0:127];
//     reg[15:0]   qpn_nums = 1;
//     reg[15:0]   rdma_cmd_nums = 11; 
//     reg[15:0]   mem_block_nums = 1;
//     reg[15:0]   check_mem_nums = 11;
//     integer i;

//     initial begin
//         rdma_config[0] = {96'h0,check_mem_nums,mem_block_nums,rdma_cmd_nums,qpn_nums};
//         rdma_config[1] = {32'd0,32'd3,32'd200000,32'd960,32'd1};

//         rdma_config[2] = {32'd64,48'd200000,48'd960,24'd1,2'd1,1'd1};
//         rdma_config[3] = {32'd128,48'd200128,48'd960,24'd1,2'd1,1'd1};
//         rdma_config[4] = {32'd1344,48'd201024,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[5] = {32'd1408,48'd208000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[6] = {32'd1472,48'd216000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[7] = {32'd2752,48'd224000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[8] = {32'd2816,48'd232000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[9] = {32'd2880,48'd240000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[10] = {32'd4096,48'd248000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[11] = {32'd4224,48'd256000,48'd1024,24'd1,2'd1,1'd1};
//         rdma_config[12] = {32'd40000,48'd264000,48'd1024,24'd1,2'd1,1'd1};        

//         rdma_config[13] = {32'd0,32'd3,32'd64,32'd200000,32'd0};
//         rdma_config[14] = {32'd0,32'd3,32'd128,32'd200128,32'd0};
//         rdma_config[15] = {32'd0,32'd4,32'd1344,32'd201024,32'd0};
//         rdma_config[16] = {32'd0,32'd4,32'd1408,32'd208000,32'd0};
//         rdma_config[17] = {32'd0,32'd4,32'd1472,32'd216000,32'd0};
//         rdma_config[18] = {32'd0,32'd4,32'd2752,32'd224000,32'd0};
//         rdma_config[19] = {32'd0,32'd4,32'd2816,32'd232000,32'd0};
//         rdma_config[20] = {32'd0,32'd4,32'd2880,32'd240000,32'd0};
//         rdma_config[21] = {32'd0,32'd4,32'd4096,32'd248000,32'd0};
//         rdma_config[22] = {32'd0,32'd4,32'd4224,32'd256000,32'd0};
//         rdma_config[23] = {32'd0,32'd4,32'd40000,32'd264000,32'd0};        
//         $writememh("config_base_write.hex",rdma_config);
//     end


// endmodule


// module tb_rdma_config_init(

//     );

//     reg[159:0]   rdma_config[0:127];
//     reg[15:0]   qpn_nums = 1;
//     reg[15:0]   rdma_cmd_nums = 11; 
//     reg[15:0]   mem_block_nums = 1;
//     reg[15:0]   check_mem_nums = 11;
//     integer i;

//     initial begin
//         rdma_config[0] = {96'h0,check_mem_nums,mem_block_nums,rdma_cmd_nums,qpn_nums};
//         rdma_config[1] = {32'd0,32'd3,32'd200000,32'd960,32'd1};
//         rdma_config[2] = {32'd64,48'd960,48'd64,24'd1,2'd0,1'd0};
//         rdma_config[3] = {32'd128,48'd960,48'd512,24'd1,2'd0,1'd0};
//         rdma_config[4] = {32'd1344,48'd1024,48'd1024,24'd1,2'd0,1'd0};
//         rdma_config[5] = {32'd1408,48'd1024,48'd8000,24'd1,2'd0,1'd0};
//         rdma_config[6] = {32'd1472,48'd1024,48'd16000,24'd1,2'd0,1'd0};
//         rdma_config[7] = {32'd2752,48'd1024,48'd24000,24'd1,2'd0,1'd0};
//         rdma_config[8] = {32'd2816,48'd1024,48'd32000,24'd1,2'd0,1'd0};
//         rdma_config[9] = {32'd2880,48'd1024,48'd40000,24'd1,2'd0,1'd0};
//         rdma_config[10] = {32'd4096,48'd1024,48'd48000,24'd1,2'd0,1'd0};
//         rdma_config[11] = {32'd4224,48'd1024,48'd56000,24'd1,2'd0,1'd0};
//         rdma_config[12] = {32'd40000,48'd1024,48'd64000,24'd1,2'd0,1'd0};        

//         rdma_config[13] = {32'd0,32'd3,32'd64,32'd64,32'd0};
//         rdma_config[14] = {32'd0,32'd3,32'd128,32'd512,32'd0};
//         rdma_config[15] = {32'd0,32'd4,32'd1344,32'd1024,32'd0};
//         rdma_config[16] = {32'd0,32'd4,32'd1408,32'd8000,32'd0};
//         rdma_config[17] = {32'd0,32'd4,32'd1472,32'd16000,32'd0};
//         rdma_config[18] = {32'd0,32'd4,32'd2752,32'd24000,32'd0};
//         rdma_config[19] = {32'd0,32'd4,32'd2816,32'd32000,32'd0};
//         rdma_config[20] = {32'd0,32'd4,32'd2880,32'd40000,32'd0};
//         rdma_config[21] = {32'd0,32'd4,32'd4096,32'd48000,32'd0};
//         rdma_config[22] = {32'd0,32'd4,32'd4224,32'd56000,32'd0};
//         rdma_config[23] = {32'd0,32'd4,32'd40000,32'd64000,32'd0};      
//         $writememh("config_base_read.hex",rdma_config);
//     end


// endmodule


// module tb_rdma_config_init(

//     );

//     reg[159:0]   rdma_config[0:127];
//     reg[15:0]   qpn_nums = 1;
//     reg[15:0]   rdma_cmd_nums = 2; 
//     reg[15:0]   mem_block_nums = 1;
//     reg[15:0]   check_mem_nums = 2;
//     integer i;

//     initial begin
//         rdma_config[0] = {96'h0,check_mem_nums,mem_block_nums,rdma_cmd_nums,qpn_nums};
//         rdma_config[1] = {32'd0,32'd3,32'h100000,32'd960,32'd1};//res,offset,length,start_addr,node_idx
//         rdma_config[2] = {32'h100000,48'd960,48'd64,24'd1,2'd0,2'd0};//length, r_addr, l_vaddr, l_qpn, rdma_cmd: 0:read 1:writre, node_idx 


//         rdma_config[3] = {32'h100000,48'h200000,48'd960,24'd2,2'd1,2'd1};
       

//         rdma_config[4] = {32'd0,32'd3,32'h100000,32'd64,32'd0};


//         rdma_config[5] = {32'd0,32'd3,32'h100000,32'h200000,32'd0};    
//         $writememh("config_big_rd_wr.hex",rdma_config);
//     end


// endmodule


module tb_rdma_config_init(

    );

    reg[159:0]   rdma_config[0:127];
    reg[15:0]   qpn_nums = 1;
    reg[15:0]   rdma_cmd_nums = 2; 
    reg[15:0]   mem_block_nums = 1;
    reg[15:0]   check_mem_nums = 2;
    integer i;

    initial begin
        rdma_config[0] = {96'h0,check_mem_nums,mem_block_nums,rdma_cmd_nums,qpn_nums};
        rdma_config[1] = {32'd0,32'd3,32'h100000,32'd960,32'd1};//res,offset,length,start_addr,node_idx
        rdma_config[2] = {32'h100000,48'd960,48'd64,24'd1,2'd0,2'd0};//length, r_addr, l_vaddr, l_qpn, rdma_cmd: 0:read 1:writre, node_idx 


        rdma_config[3] = {32'h100000,48'h200000,48'd960,24'd2,2'd2,2'd1};
       

        rdma_config[4] = {32'd0,32'd3,32'h100000,32'd64,32'd0};


        rdma_config[5] = {32'd0,32'd3,32'h100000,32'h200000,32'd0};    
        $writememh("config_big_rd_wr.hex",rdma_config);
    end


endmodule



module testbench_IP_LOOP(

    );

    reg                 clock                         =0;
    reg                 reset                         =0;
    wire                io_s_tx_meta_0_ready          ;
    reg                 io_s_tx_meta_0_valid          =0;
    reg       [1:0]     io_s_tx_meta_0_bits_rdma_cmd  =0;
    reg       [23:0]    io_s_tx_meta_0_bits_qpn       =0;
    reg       [47:0]    io_s_tx_meta_0_bits_local_vaddr=0;
    reg       [47:0]    io_s_tx_meta_0_bits_remote_vaddr=0;
    reg       [31:0]    io_s_tx_meta_0_bits_length    =0;
    wire                io_s_tx_meta_1_ready          ;
    reg                 io_s_tx_meta_1_valid          =0;
    reg       [1:0]     io_s_tx_meta_1_bits_rdma_cmd  =0;
    reg       [23:0]    io_s_tx_meta_1_bits_qpn       =0;
    reg       [47:0]    io_s_tx_meta_1_bits_local_vaddr=0;
    reg       [47:0]    io_s_tx_meta_1_bits_remote_vaddr=0;
    reg       [31:0]    io_s_tx_meta_1_bits_length    =0;
    wire                io_s_send_data_0_ready        ;
    reg                 io_s_send_data_0_valid        =0;
    reg                 io_s_send_data_0_bits_last    =0;
    reg       [511:0]   io_s_send_data_0_bits_data    =0;
    reg       [63:0]    io_s_send_data_0_bits_keep    =0;
    wire                io_s_send_data_1_ready        ;
    reg                 io_s_send_data_1_valid        =0;
    reg                 io_s_send_data_1_bits_last    =0;
    reg       [511:0]   io_s_send_data_1_bits_data    =0;
    reg       [63:0]    io_s_send_data_1_bits_keep    =0;
    reg                 io_m_recv_data_0_ready        =0;
    wire                io_m_recv_data_0_valid        ;
    wire                io_m_recv_data_0_bits_last    ;
    wire      [511:0]   io_m_recv_data_0_bits_data    ;
    wire      [63:0]    io_m_recv_data_0_bits_keep    ;
    reg                 io_m_recv_data_1_ready        =0;
    wire                io_m_recv_data_1_valid        ;
    wire                io_m_recv_data_1_bits_last    ;
    wire      [511:0]   io_m_recv_data_1_bits_data    ;
    wire      [63:0]    io_m_recv_data_1_bits_keep    ;
    reg                 io_m_recv_meta_0_ready        =0;
    wire                io_m_recv_meta_0_valid        ;
    wire      [15:0]    io_m_recv_meta_0_bits_qpn     ;
    wire      [23:0]    io_m_recv_meta_0_bits_msg_num ;
    wire      [20:0]    io_m_recv_meta_0_bits_pkg_num ;
    wire      [20:0]    io_m_recv_meta_0_bits_pkg_total;
    reg                 io_m_recv_meta_1_ready        =0;
    wire                io_m_recv_meta_1_valid        ;
    wire      [15:0]    io_m_recv_meta_1_bits_qpn     ;
    wire      [23:0]    io_m_recv_meta_1_bits_msg_num ;
    wire      [20:0]    io_m_recv_meta_1_bits_pkg_num ;
    wire      [20:0]    io_m_recv_meta_1_bits_pkg_total;
    reg                 io_m_cmpt_meta_0_ready        =0;
    wire                io_m_cmpt_meta_0_valid        ;
    wire      [15:0]    io_m_cmpt_meta_0_bits_qpn     ;
    wire      [23:0]    io_m_cmpt_meta_0_bits_msg_num ;
    wire      [1:0]     io_m_cmpt_meta_0_bits_msg_type;
    reg                 io_m_cmpt_meta_1_ready        =0;
    wire                io_m_cmpt_meta_1_valid        ;
    wire      [15:0]    io_m_cmpt_meta_1_bits_qpn     ;
    wire      [23:0]    io_m_cmpt_meta_1_bits_msg_num ;
    wire      [1:0]     io_m_cmpt_meta_1_bits_msg_type;
    reg                 io_m_mem_read_cmd_0_ready     =0;
    wire                io_m_mem_read_cmd_0_valid     ;
    wire      [63:0]    io_m_mem_read_cmd_0_bits_vaddr;
    wire      [31:0]    io_m_mem_read_cmd_0_bits_length;
    reg                 io_m_mem_read_cmd_1_ready     =0;
    wire                io_m_mem_read_cmd_1_valid     ;
    wire      [63:0]    io_m_mem_read_cmd_1_bits_vaddr;
    wire      [31:0]    io_m_mem_read_cmd_1_bits_length;
    wire                io_s_mem_read_data_0_ready    ;
    reg                 io_s_mem_read_data_0_valid    =0;
    reg                 io_s_mem_read_data_0_bits_last=0;
    reg       [511:0]   io_s_mem_read_data_0_bits_data=0;
    reg       [63:0]    io_s_mem_read_data_0_bits_keep=0;
    wire                io_s_mem_read_data_1_ready    ;
    reg                 io_s_mem_read_data_1_valid    =0;
    reg                 io_s_mem_read_data_1_bits_last=0;
    reg       [511:0]   io_s_mem_read_data_1_bits_data=0;
    reg       [63:0]    io_s_mem_read_data_1_bits_keep=0;
    reg                 io_m_mem_write_cmd_0_ready    =0;
    wire                io_m_mem_write_cmd_0_valid    ;
    wire      [63:0]    io_m_mem_write_cmd_0_bits_vaddr;
    wire      [31:0]    io_m_mem_write_cmd_0_bits_length;
    reg                 io_m_mem_write_cmd_1_ready    =0;
    wire                io_m_mem_write_cmd_1_valid    ;
    wire      [63:0]    io_m_mem_write_cmd_1_bits_vaddr;
    wire      [31:0]    io_m_mem_write_cmd_1_bits_length;
    reg                 io_m_mem_write_data_0_ready   =0;
    wire                io_m_mem_write_data_0_valid   ;
    wire                io_m_mem_write_data_0_bits_last;
    wire      [511:0]   io_m_mem_write_data_0_bits_data;
    wire      [63:0]    io_m_mem_write_data_0_bits_keep;
    reg                 io_m_mem_write_data_1_ready   =0;
    wire                io_m_mem_write_data_1_valid   ;
    wire                io_m_mem_write_data_1_bits_last;
    wire      [511:0]   io_m_mem_write_data_1_bits_data;
    wire      [63:0]    io_m_mem_write_data_1_bits_keep;
    wire                io_qp_init_0_ready            ;
    reg                 io_qp_init_0_valid            =0;
    reg       [15:0]    io_qp_init_0_bits_qpn         =0;
    reg       [23:0]    io_qp_init_0_bits_local_psn   =0;
    reg       [23:0]    io_qp_init_0_bits_remote_psn  =0;
    reg       [23:0]    io_qp_init_0_bits_remote_qpn  =0;
    reg       [31:0]    io_qp_init_0_bits_remote_ip   =0;
    reg       [15:0]    io_qp_init_0_bits_remote_udp_port=0;
    reg       [23:0]    io_qp_init_0_bits_credit      =0;
    wire                io_qp_init_1_ready            ;
    reg                 io_qp_init_1_valid            =0;
    reg       [15:0]    io_qp_init_1_bits_qpn         =0;
    reg       [23:0]    io_qp_init_1_bits_local_psn   =0;
    reg       [23:0]    io_qp_init_1_bits_remote_psn  =0;
    reg       [23:0]    io_qp_init_1_bits_remote_qpn  =0;
    reg       [31:0]    io_qp_init_1_bits_remote_ip   =0;
    reg       [15:0]    io_qp_init_1_bits_remote_udp_port=0;
    reg       [23:0]    io_qp_init_1_bits_credit      =0;
    reg       [31:0]    io_local_ip_address_0         =0;
    reg       [31:0]    io_local_ip_address_1         =0;
    wire      [31:0]    io_reports                    ;

IN#(154)in_io_s_tx_meta_0(
        clock,
        reset,
        {io_s_tx_meta_0_bits_rdma_cmd,io_s_tx_meta_0_bits_qpn,io_s_tx_meta_0_bits_local_vaddr,io_s_tx_meta_0_bits_remote_vaddr,io_s_tx_meta_0_bits_length},
        io_s_tx_meta_0_valid,
        io_s_tx_meta_0_ready
);
// rdma_cmd, qpn, local_vaddr, remote_vaddr, length
// 2'h0, 24'h0, 48'h0, 48'h0, 32'h0

IN#(154)in_io_s_tx_meta_1(
        clock,
        reset,
        {io_s_tx_meta_1_bits_rdma_cmd,io_s_tx_meta_1_bits_qpn,io_s_tx_meta_1_bits_local_vaddr,io_s_tx_meta_1_bits_remote_vaddr,io_s_tx_meta_1_bits_length},
        io_s_tx_meta_1_valid,
        io_s_tx_meta_1_ready
);
// rdma_cmd, qpn, local_vaddr, remote_vaddr, length
// 2'h0, 24'h0, 48'h0, 48'h0, 32'h0

IN#(577)in_io_s_send_data_0(
        clock,
        reset,
        {io_s_send_data_0_bits_last,io_s_send_data_0_bits_data,io_s_send_data_0_bits_keep},
        io_s_send_data_0_valid,
        io_s_send_data_0_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

// IN#(577)in_io_s_send_data_1(
//         clock,
//         reset,
//         {io_s_send_data_1_bits_last,io_s_send_data_1_bits_data,io_s_send_data_1_bits_keep},
//         io_s_send_data_1_valid,
//         io_s_send_data_1_ready
// );
// last, data, keep
// 1'h0, 512'h0, 64'h0

OUT#(577)out_io_m_recv_data_0(
        clock,
        reset,
        {io_m_recv_data_0_bits_last,io_m_recv_data_0_bits_data,io_m_recv_data_0_bits_keep},
        io_m_recv_data_0_valid,
        io_m_recv_data_0_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

OUT#(577)out_io_m_recv_data_1(
        clock,
        reset,
        {io_m_recv_data_1_bits_last,io_m_recv_data_1_bits_data,io_m_recv_data_1_bits_keep},
        io_m_recv_data_1_valid,
        io_m_recv_data_1_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

OUT#(82)out_io_m_recv_meta_0(
        clock,
        reset,
        {io_m_recv_meta_0_bits_qpn,io_m_recv_meta_0_bits_msg_num,io_m_recv_meta_0_bits_pkg_num,io_m_recv_meta_0_bits_pkg_total},
        io_m_recv_meta_0_valid,
        io_m_recv_meta_0_ready
);
// qpn, msg_num, pkg_num, pkg_total
// 16'h0, 24'h0, 21'h0, 21'h0

OUT#(82)out_io_m_recv_meta_1(
        clock,
        reset,
        {io_m_recv_meta_1_bits_qpn,io_m_recv_meta_1_bits_msg_num,io_m_recv_meta_1_bits_pkg_num,io_m_recv_meta_1_bits_pkg_total},
        io_m_recv_meta_1_valid,
        io_m_recv_meta_1_ready
);
// qpn, msg_num, pkg_num, pkg_total
// 16'h0, 24'h0, 21'h0, 21'h0

OUT#(42)out_io_m_cmpt_meta_0(
        clock,
        reset,
        {io_m_cmpt_meta_0_bits_qpn,io_m_cmpt_meta_0_bits_msg_num,io_m_cmpt_meta_0_bits_msg_type},
        io_m_cmpt_meta_0_valid,
        io_m_cmpt_meta_0_ready
);
// qpn, msg_num, msg_type
// 16'h0, 24'h0, 2'h0

OUT#(42)out_io_m_cmpt_meta_1(
        clock,
        reset,
        {io_m_cmpt_meta_1_bits_qpn,io_m_cmpt_meta_1_bits_msg_num,io_m_cmpt_meta_1_bits_msg_type},
        io_m_cmpt_meta_1_valid,
        io_m_cmpt_meta_1_ready
);
// qpn, msg_num, msg_type
// 16'h0, 24'h0, 2'h0

DMA #(512) qdma0(
    clock,
    reset,
    //DMA CMD streams
    io_m_mem_read_cmd_0_valid,
    io_m_mem_read_cmd_0_ready,
    io_m_mem_read_cmd_0_bits_vaddr,
    io_m_mem_read_cmd_0_bits_length,
    io_m_mem_write_cmd_0_valid,
    io_m_mem_write_cmd_0_ready,
    io_m_mem_write_cmd_0_bits_vaddr,
    io_m_mem_write_cmd_0_bits_length,        
    //DMA Data streams      
    io_s_mem_read_data_0_valid,
    io_s_mem_read_data_0_ready,
    io_s_mem_read_data_0_bits_data,
    io_s_mem_read_data_0_bits_keep,
    io_s_mem_read_data_0_bits_last,
    io_m_mem_write_data_0_valid,
    io_m_mem_write_data_0_ready,
    io_m_mem_write_data_0_bits_data,
    io_m_mem_write_data_0_bits_keep,
    io_m_mem_write_data_0_bits_last        
);


DMA #(512) qdma1(
    clock,
    reset,
    //DMA CMD streams
    io_m_mem_read_cmd_1_valid,
    io_m_mem_read_cmd_1_ready,
    io_m_mem_read_cmd_1_bits_vaddr,
    io_m_mem_read_cmd_1_bits_length,
    io_m_mem_write_cmd_1_valid,
    io_m_mem_write_cmd_1_ready,
    io_m_mem_write_cmd_1_bits_vaddr,
    io_m_mem_write_cmd_1_bits_length,        
    //DMA Data streams      
    io_s_mem_read_data_1_valid,
    io_s_mem_read_data_1_ready,
    io_s_mem_read_data_1_bits_data,
    io_s_mem_read_data_1_bits_keep,
    io_s_mem_read_data_1_bits_last,
    io_m_mem_write_data_1_valid,
    io_m_mem_write_data_1_ready,
    io_m_mem_write_data_1_bits_data,
    io_m_mem_write_data_1_bits_keep,
    io_m_mem_write_data_1_bits_last        
);


reg                 io_m_mem_read_cmd_2_ready     ;
reg                io_m_mem_read_cmd_2_valid     =0;
reg      [63:0]    io_m_mem_read_cmd_2_bits_vaddr=0;
reg      [31:0]    io_m_mem_read_cmd_2_bits_length=0;
reg                 io_m_mem_write_cmd_2_ready    ;
wire                io_m_mem_write_cmd_2_valid    =0;
wire      [63:0]    io_m_mem_write_cmd_2_bits_vaddr=0;
wire      [31:0]    io_m_mem_write_cmd_2_bits_length=0;
reg                 io_m_mem_write_data_2_ready   ;
wire                io_m_mem_write_data_2_valid   =0;
wire                io_m_mem_write_data_2_bits_last=0;
wire      [511:0]   io_m_mem_write_data_2_bits_data=0;
wire      [63:0]    io_m_mem_write_data_2_bits_keep=0;



DMA #(512) qdma2(
    clock,
    reset,
    //DMA CMD streams
    io_m_mem_read_cmd_2_valid,
    io_m_mem_read_cmd_2_ready,
    io_m_mem_read_cmd_2_bits_vaddr,
    io_m_mem_read_cmd_2_bits_length,
    io_m_mem_write_cmd_2_valid,
    io_m_mem_write_cmd_2_ready,
    io_m_mem_write_cmd_2_bits_vaddr,
    io_m_mem_write_cmd_2_bits_length,        
    //DMA Data streams      
    io_s_send_data_1_valid,
    io_s_send_data_1_ready,
    io_s_send_data_1_bits_data,
    io_s_send_data_1_bits_keep,
    io_s_send_data_1_bits_last,
    io_m_mem_write_data_2_valid,
    io_m_mem_write_data_2_ready,
    io_m_mem_write_data_2_bits_data,
    io_m_mem_write_data_2_bits_keep,
    io_m_mem_write_data_2_bits_last        
);

IN#(160)in_io_qp_init_0(
        clock,
        reset,
        {io_qp_init_0_bits_qpn,io_qp_init_0_bits_local_psn,io_qp_init_0_bits_remote_psn,io_qp_init_0_bits_remote_qpn,io_qp_init_0_bits_remote_ip,io_qp_init_0_bits_remote_udp_port,io_qp_init_0_bits_credit},
        io_qp_init_0_valid,
        io_qp_init_0_ready
);
// qpn, local_psn, remote_psn, remote_qpn, remote_ip, remote_udp_port, credit
// 16'h0, 24'h0, 24'h0, 24'h0, 32'h0, 16'h0, 24'h0

IN#(160)in_io_qp_init_1(
        clock,
        reset,
        {io_qp_init_1_bits_qpn,io_qp_init_1_bits_local_psn,io_qp_init_1_bits_remote_psn,io_qp_init_1_bits_remote_qpn,io_qp_init_1_bits_remote_ip,io_qp_init_1_bits_remote_udp_port,io_qp_init_1_bits_credit},
        io_qp_init_1_valid,
        io_qp_init_1_ready
);
// qpn, local_psn, remote_psn, remote_qpn, remote_ip, remote_udp_port, credit
// 16'h0, 24'h0, 24'h0, 24'h0, 32'h0, 16'h0, 24'h0


IP_LOOP IP_LOOP_inst(
        .*
);

tb_rdma_config_init tb_rdma_config_init();

reg[159:0]   rdma_config[0:1023];
reg[15:0]   qpn_nums, rdma_cmd_nums, mem_block_nums, check_mem_nums;
reg[31:0]   start_addr, length, offset;
integer i;


//test read write
// initial begin
//         reset <= 1;
//         clock = 1;
//         $readmemh("config_big_rd_wr.hex", rdma_config);
//         #1000;
//         reset <= 0;
//         #100;
//         out_io_m_recv_data_0.start();
//         out_io_m_recv_data_1.start();
//         out_io_m_recv_meta_0.start();
//         out_io_m_recv_meta_1.start();
//         out_io_m_cmpt_meta_0.start();
//         out_io_m_cmpt_meta_1.start();
//         #100
//         qpn_nums        = rdma_config[0][15:0];
//         rdma_cmd_nums   = rdma_config[0][31:16];
//         mem_block_nums  = rdma_config[0][47:32];
//         check_mem_nums  = rdma_config[0][63:48];
//         for(i=0;i<mem_block_nums;i=i+1)begin
//             if(rdma_config[i+1][1:0])begin
//                 qdma1.init_incr(rdma_config[i+1][63:32],rdma_config[i+1][95:64],rdma_config[i+1][127:96]);
//             end
//             else begin
//                 qdma0.init_incr(rdma_config[i+1][63:32],rdma_config[i+1][95:64],rdma_config[i+1][127:96]);
//             end
//         end


//         #200;
//         in_io_fc_init_0.write({24'd1,8'd0,16'd1600,24'd0,1'b0});     // qpn, op_code, credit, psn// 24'h0, 8'h0, 16'h0, 24'h0
//         in_io_fc_init_1.write({24'd2,8'd0,16'd1600,24'd0,1'b0});

//         in_io_conn_init_0.write({24'd1,24'd2,32'h01bda8c0,16'd17});//l_qpn,r_qpn,r_ip,r_udup_port
//         in_io_conn_init_1.write({24'd2,24'd1,32'h02bda8c0,16'd17});

//         in_io_psn_init_0.write({24'd1,24'd1000,24'd4000});// qpn, local_psn, remote_psn
//         in_io_psn_init_1.write({24'd2,24'd4000,24'd1000});// 24'h0, 24'h0, 24'h0

//         #2000
//         for(i=0;i<rdma_cmd_nums;i=i+1)begin
//             if(rdma_config[i+mem_block_nums+1][1:0])begin
//                 in_io_s_tx_meta_1.write({rdma_config[i+mem_block_nums+1][3:2],rdma_config[i+mem_block_nums+1][27:4],rdma_config[i+mem_block_nums+1][75:28],rdma_config[i+mem_block_nums+1][123:76],rdma_config[i+mem_block_nums+1][155:124]});
//             end
//             else begin
//                 in_io_s_tx_meta_0.write({rdma_config[i+mem_block_nums+1][3:2],rdma_config[i+mem_block_nums+1][27:4],rdma_config[i+mem_block_nums+1][75:28],rdma_config[i+mem_block_nums+1][123:76],rdma_config[i+mem_block_nums+1][155:124]});
//             end
//         end
//         #1000000
//         for(i=0;i<check_mem_nums;i=i+1)begin
//             if(rdma_config[i+mem_block_nums+rdma_cmd_nums+1][1:0])begin
//                 qdma1.check_mem(rdma_config[i+mem_block_nums+rdma_cmd_nums+1][63:32],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][95:64],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][127:96]);
//             end
//             else begin
//                 qdma0.check_mem(rdma_config[i+mem_block_nums+rdma_cmd_nums+1][63:32],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][95:64],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][127:96]);
//             end
//         end

// end

//test send

initial begin
    reset <= 1;
    clock = 1;
    $readmemh("config_big_rd_wr.hex", rdma_config);
    #1000;
    reset <= 0;
    #100;
    out_io_m_recv_data_0.start();
    out_io_m_recv_data_1.start();
    out_io_m_recv_meta_0.start();
    out_io_m_recv_meta_1.start();
    out_io_m_cmpt_meta_0.start();
    out_io_m_cmpt_meta_1.start();
    #100
    qpn_nums        = rdma_config[0][15:0];
    rdma_cmd_nums   = rdma_config[0][31:16];
    mem_block_nums  = rdma_config[0][47:32];
    check_mem_nums  = rdma_config[0][63:48];
    for(i=0;i<mem_block_nums;i=i+1)begin
        if(rdma_config[i+1][1:0])begin
            qdma1.init_incr(rdma_config[i+1][63:32],rdma_config[i+1][95:64],rdma_config[i+1][127:96]);
        end
        else begin
            qdma0.init_incr(rdma_config[i+1][63:32],rdma_config[i+1][95:64],rdma_config[i+1][127:96]);
        end
    end


    #200;
    in_io_qp_init_0.write({16'd1,24'd1000,24'd4000,24'd2,32'h01bda8c0,16'd17,24'd1600});     // qpn, op_code, credit, psn// 24'h0, 8'h0, 16'h0, 24'h0
    in_io_qp_init_1.write({16'd2,24'd4000,24'd1000,24'd1,32'h02bda8c0,16'd17,24'd1600});

// qpn, local_psn, remote_psn, remote_qpn, remote_ip, remote_udp_port, credit
// 16'h0, 24'h0, 24'h0, 24'h0, 32'h0, 16'h0, 24'h0

    #2000
    for(i=0;i<rdma_cmd_nums;i=i+1)begin
        if(rdma_config[i+mem_block_nums+1][1:0])begin
            in_io_s_tx_meta_1.write({rdma_config[i+mem_block_nums+1][3:2],rdma_config[i+mem_block_nums+1][27:4],rdma_config[i+mem_block_nums+1][75:28],rdma_config[i+mem_block_nums+1][123:76],rdma_config[i+mem_block_nums+1][155:124]});// rdma_cmd, qpn, local_vaddr, remote_vaddr, length
            if(rdma_config[i+mem_block_nums+1][3:2]==2)begin
                io_m_mem_read_cmd_2_valid       = 1;
                io_m_mem_read_cmd_2_bits_vaddr  = rdma_config[i+mem_block_nums+1][75:28];
                io_m_mem_read_cmd_2_bits_length = rdma_config[i+mem_block_nums+1][155:124];                
            end
        end
        else begin
            in_io_s_tx_meta_0.write({rdma_config[i+mem_block_nums+1][3:2],rdma_config[i+mem_block_nums+1][27:4],rdma_config[i+mem_block_nums+1][75:28],rdma_config[i+mem_block_nums+1][123:76],rdma_config[i+mem_block_nums+1][155:124]});
            if(rdma_config[i+mem_block_nums+1][3:2]==2)begin
                io_m_mem_read_cmd_2_valid       = 1;
                io_m_mem_read_cmd_2_bits_vaddr  = rdma_config[i+mem_block_nums+1][75:28];
                io_m_mem_read_cmd_2_bits_length = rdma_config[i+mem_block_nums+1][155:124];                
            end            
        end
    end
    #10
    io_m_mem_read_cmd_2_valid       = 0;
    #1000000
    for(i=0;i<check_mem_nums;i=i+1)begin
        if(rdma_config[i+mem_block_nums+rdma_cmd_nums+1][1:0])begin
            qdma1.check_mem(rdma_config[i+mem_block_nums+rdma_cmd_nums+1][63:32],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][95:64],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][127:96]);
        end
        else begin
            qdma0.check_mem(rdma_config[i+mem_block_nums+rdma_cmd_nums+1][63:32],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][95:64],rdma_config[i+mem_block_nums+rdma_cmd_nums+1][127:96]);
        end
    end

end



always #5 clock=~clock;

endmodule