package examples

import chisel3._
import chisel3.stage._
import chisel3.util._

//  class axi_crossbar extends Module with mips_macros{
//         val io = IO(new Bundle{
          
        
//         val aclk           =   Input(UInt(1.W) )              // input wire aclk
//         val aresetn        =   Input(UInt(1.W) )               // input wire aresetn

//         val s_axi_awid     =   Input(UInt(8.W) )       
//         val s_axi_awaddr   =   Input(UInt(64.W) )       
//         val s_axi_awlen    =   Input(UInt(16.W) )       
//         val s_axi_awsize   =   Input(UInt(6.W) )       
//         val s_axi_awburst  =   Input(UInt(4.W) )       
//         val s_axi_awlock   =   Input(UInt(4.W) )       
//         val s_axi_awcache  =   Input(UInt(8.W) )       
//         val s_axi_awprot   =   Input(UInt(6.W) )       
//         val s_axi_awqos    =   Input(UInt(8.W) )       
//         val s_axi_awvalid  =   Input(UInt(2.W)) 
//         val s_axi_awready  =   Output(UInt(2.W) ) 

//         // val s_axi_wid      =   Input(UInt(8.W) )       
//         val s_axi_wdata    =   Input(UInt(64.W) )       
//         val s_axi_wstrb    =   Input(UInt(8.W) )       
//         val s_axi_wlast    =   Input(UInt(2.W) )       
//         val s_axi_wvalid   =   Input(UInt(2.W) )       
//         val s_axi_wready   =   Output(UInt(2.W) )       
//         val s_axi_bid      =   Output(UInt(8.W) )       
//         val s_axi_bresp    =   Output(UInt(4.W) )       
//         val s_axi_bvalid   =   Output(UInt(2.W) )      

//         val s_axi_bready   =   Input(UInt(2.W))        
//         val s_axi_arid     =   Input(UInt(8.W))        
//         val s_axi_araddr   =   Input(UInt(64.W))        
//         val s_axi_arlen    =   Input(UInt(16.W))        
//         val s_axi_arsize   =   Input(UInt(6.W))        
//         val s_axi_arburst  =   Input(UInt(4.W))        
//         val s_axi_arlock   =   Input(UInt(4.W))        
//         val s_axi_arcache  =   Input(UInt(8.W))        
//         val s_axi_arprot   =   Input(UInt(6.W))       
//         val s_axi_arqos    =   Input(UInt(8.W))        
//         val s_axi_arvalid  =   Input(UInt(2.W))        
//         val s_axi_arready  =   Output(UInt(2.W))        
//         val s_axi_rid      =   Output(UInt(8.W))        
//         val s_axi_rdata    =   Output(UInt(64.W))       
//         val s_axi_rresp    =   Output(UInt(4.W))        
//         val s_axi_rlast    =   Output(UInt(2.W))        
//         val s_axi_rvalid   =   Output(UInt(2.W))    
        
//         val s_axi_rready   =   Input(UInt(2.W))     

        
//         val m_axi_awid     =   Output(UInt(4.W))        
//         val m_axi_awaddr   =   Output(UInt(32.W) )        
//         val m_axi_awlen    =   Output(UInt(8.W) )        
//         val m_axi_awsize   =   Output(UInt(3.W) )        
//         val m_axi_awburst  =   Output(UInt(2.W) )        
//         val m_axi_awlock   =   Output(UInt(2.W) )        
//         val m_axi_awcache  =   Output(UInt(4.W) )        
//         val m_axi_awprot   =   Output(UInt(3.W) )        
//         val m_axi_awqos    =   Output(UInt(4.W) )        
//         val m_axi_awvalid  =   Output(UInt(1.W) )        
        
//         val m_axi_awready  =   Input(UInt(1.W) )        
//         val m_axi_wid      =   Output(UInt(4.W) )        
//         val m_axi_wdata    =   Output(UInt(32.W) )        
//         val m_axi_wstrb    =   Output(UInt(4.W) )        
//         val m_axi_wlast    =   Output(UInt(1.W) )        
//         val m_axi_wvalid   =   Output(UInt(1.W) ) 

//         val m_axi_wready   =   Input(UInt(1.W) )        
//         val m_axi_bid      =   Input(UInt(4.W) )        
//         val m_axi_bresp    =   Input(UInt(2.W) )        
//         val m_axi_bvalid   =   Input(UInt(1.W))  


//         val m_axi_bready   =   Output(UInt(1.W) )        
//         val m_axi_arid     =   Output(UInt(4.W) )        
//         val m_axi_araddr   =   Output(UInt(32.W) )        
//         val m_axi_arlen    =   Output(UInt(8.W) )        
//         val m_axi_arsize   =   Output(UInt(3.W) )        
//         val m_axi_arburst  =   Output(UInt(2.W) )        
//         val m_axi_arlock   =   Output(UInt(2.W) )        
//         val m_axi_arcache  =   Output(UInt(4.W) )        
//         val m_axi_arprot   =   Output(UInt(3.W) )        
//         val m_axi_arqos    =   Output(UInt(4.W) )        
//         val m_axi_arvalid  =   Output(UInt(1.W) )        
//         val m_axi_rready   =   Output(UInt(1.W))  

//         val m_axi_arready  =   Input(UInt(1.W) )        
//         val m_axi_rid      =   Input(UInt(4.W) )        
//         val m_axi_rdata    =   Input(UInt(32.W) )        
//         val m_axi_rresp    =   Input(UInt(2.W))        
//         val m_axi_rlast    =   Input(UInt(1.W))         
//         val m_axi_rvalid   =   Input(UInt(1.W))       
    
//         })
//         val  cache_write_sel  = RegInit(0.U(1.W))
//         val  cache_read_sel  = RegInit(0.U(1.W))
//         val  cache_read_sel_change_enable  = RegInit(0.U(1.W))
//         val  cache_write_sel_change_enable  = RegInit(0.U(1.W))
//         //cache_read_sel_change_enable := Mux(io.s_axi_arvalid =/= 0.U.asBool,Mux(io.m_axi_rlast.asBool,1.U.asBool,0.U.asBool),0.U)
//         cache_read_sel_change_enable := Mux(io.m_axi_rlast.asBool,1.U,Mux(io.s_axi_arvalid =/= 0.U,0.U,cache_read_sel_change_enable))
//         cache_read_sel := Mux(cache_read_sel_change_enable.asBool,Mux(io.s_axi_arvalid(1),1.U,0.U),cache_read_sel)

//         //cache_write_sel_change_enable := Mux(io.s_axi_awvalid =/= 0.U.asBool,0.U,Mux(io.m_axi_wlast.asBool,1.U.asBool,0.U.asBool))
//         cache_write_sel_change_enable := Mux(io.m_axi_wlast.asBool,1.U,Mux(io.s_axi_awvalid =/= 0.U,0.U,cache_write_sel_change_enable))
//         cache_write_sel := Mux(cache_write_sel_change_enable.asBool,Mux(io.s_axi_awvalid(1),1.U,0.U),cache_write_sel)

//         io.m_axi_awid     := Mux(cache_write_sel.asBool,io.s_axi_awid(7,4),io.s_axi_awid(3,0))
//         io.m_axi_awaddr   := Mux(cache_write_sel.asBool,io.s_axi_awaddr(63,32),io.s_axi_awaddr(31,0))
//         io.m_axi_awlen    := Mux(cache_write_sel.asBool,io.s_axi_awlen(15,8),io.s_axi_awlen(7,0))
//         io.m_axi_awsize   := Mux(cache_write_sel.asBool,io.s_axi_awsize (5,3),io.s_axi_awsize (2,0))
//         io.m_axi_awburst  := Mux(cache_write_sel.asBool,io.s_axi_awburst(3,2),io.s_axi_awburst(1,0))
//         io.m_axi_awlock   := Mux(cache_write_sel.asBool,io.s_axi_awlock(3,2),io.s_axi_awlock(1,0))
//         io.m_axi_awcache  := Mux(cache_write_sel.asBool,io.s_axi_awcache(7,4),io.s_axi_awcache(3,0))
//         io.m_axi_awprot   := Mux(cache_write_sel.asBool,io.s_axi_awprot(5,3),io.s_axi_awprot(2,0))
//         io.m_axi_awqos    := Mux(cache_write_sel.asBool,io.s_axi_awqos(7,4),io.s_axi_awqos(3,0))
//         io.m_axi_awvalid  := Mux(cache_write_sel.asBool,io.s_axi_awvalid(1),io.s_axi_awvalid(0))

//         io.s_axi_awready := Cat(Mux(cache_write_sel.asBool,io.m_axi_awready,0.U),Mux(cache_write_sel.asBool,0.U,io.m_axi_awready))
//         io.s_axi_wready  := Cat(Mux(cache_write_sel.asBool,io.m_axi_wready,0.U),Mux(!cache_write_sel.asBool,io.m_axi_wready,0.U)) 
//         io.s_axi_bid      := Cat(Mux(cache_write_sel.asBool,io.m_axi_bid,0.U),Mux(!cache_write_sel.asBool,io.m_axi_bid,0.U))
//         io.s_axi_bresp    := Cat(Mux(cache_write_sel.asBool,io.m_axi_bresp,0.U),Mux(!cache_write_sel.asBool,io.m_axi_bresp,0.U))
//         io.s_axi_bvalid   := Cat(Mux(cache_write_sel.asBool,io.m_axi_bvalid,0.U),Mux(!cache_write_sel.asBool,io.m_axi_bvalid,0.U))  

//         io.m_axi_wdata    := Mux(cache_write_sel.asBool,io.s_axi_wdata(63,32),io.s_axi_wdata(31,0))
//         io.m_axi_wstrb    := Mux(cache_write_sel.asBool,io.s_axi_wstrb(7,4),io.s_axi_wstrb(3,0))
//         io.m_axi_wlast    := Mux(cache_write_sel.asBool,io.s_axi_wlast(1),io.s_axi_wlast(0))
//         io.m_axi_wvalid   := Mux(cache_write_sel.asBool,io.s_axi_wvalid(1),io.s_axi_wvalid(0))
//         io.m_axi_bready   := Mux(cache_write_sel.asBool,io.s_axi_bready(1),io.s_axi_bready(0))

//         io.m_axi_arid     := Mux(cache_read_sel.asBool,io.s_axi_arid(7,4),io.s_axi_arid(3,0))
//         io.m_axi_araddr   := Mux(cache_read_sel.asBool,io.s_axi_araddr(63,32),io.s_axi_araddr(31,0))
//         io.m_axi_arlen    := Mux(cache_read_sel.asBool,io.s_axi_arlen(15,8),io.s_axi_arlen(7,0))
//         io.m_axi_arsize   := Mux(cache_read_sel.asBool,io.s_axi_arsize(5,3),io.s_axi_arsize(2,0))
//         io.m_axi_arburst  := Mux(cache_read_sel.asBool,io.s_axi_arburst(3,2),io.s_axi_arburst(1,0))
//         io.m_axi_arlock   := Mux(cache_read_sel.asBool,io.s_axi_arlock(3,2),io.s_axi_arlock(1,0))
//         io.m_axi_arcache  := Mux(cache_read_sel.asBool,io.s_axi_arcache(7,4),io.s_axi_arcache(3,0))
//         io.m_axi_arprot   := Mux(cache_read_sel.asBool,io.s_axi_arprot(5,3),io.s_axi_arprot(2,0))
//         io.m_axi_arqos    := Mux(cache_read_sel.asBool,io.s_axi_arqos(7,4),io.s_axi_arqos(3,0))
//         io.m_axi_arvalid  := Mux(cache_read_sel.asBool,io.s_axi_arvalid(1),io.s_axi_arvalid(0))
//         io.m_axi_rready   := Mux(cache_read_sel.asBool,io.s_axi_rready(1),io.s_axi_rready(0))

//         io.s_axi_arready    := Cat(Mux(cache_read_sel.asBool,io.m_axi_arready,0.U),Mux(cache_read_sel.asBool,0.U,io.m_axi_arready))
//         io.s_axi_rid        := Cat(Mux(cache_read_sel.asBool,io.m_axi_rid,0.U),Mux(!cache_read_sel.asBool,io.m_axi_rid,0.U))
//         io.s_axi_rresp      := Cat(Mux(cache_read_sel.asBool,io.m_axi_rresp,0.U),Mux(!cache_read_sel.asBool,io.m_axi_rresp,0.U))
//         io.s_axi_rvalid     := Cat(Mux(cache_read_sel.asBool,io.m_axi_rvalid,0.U),Mux(!cache_read_sel.asBool,io.m_axi_rvalid,0.U))
//         io.s_axi_rlast      := Cat(Mux(cache_read_sel.asBool,io.m_axi_rlast,0.U),Mux(!cache_read_sel.asBool,io.m_axi_rlast,0.U))  
//         io.s_axi_rdata      := Cat(Mux(cache_read_sel.asBool,io.m_axi_rdata,0.U),Mux(!cache_read_sel.asBool,io.m_axi_rdata,0.U))  

//         io.m_axi_rready   := Mux(cache_read_sel.asBool,io.s_axi_rready(1),io.s_axi_rready(0))
//         // io.m_axi_rdata    := Mux(cache_read_sel.asBool,io.s_axi_rdata(63,32),io.s_axi_rdata(31,0))
//         // io.m_axi_rlast    := Mux(cache_read_sel.asBool,io.s_axi_rlast(1),io.s_axi_rlast(0))
//         // io.m_axi_rvalid   := Mux(cache_read_sel.asBool,io.s_axi_rvalid(1),io.s_axi_rvalid(0))
//         // io.m_axi_rready   := Mux(cache_read_sel.asBool,io.s_axi_rready(1),io.s_axi_rready(0))

//     }

     class axi_crossbar_0 extends BlackBox{
        val io = IO(new Bundle{
          
        
        val aclk           =   Input(UInt(1.W) )              // input wire aclk
        val aresetn        =   Input(UInt(1.W) )               // input wire aresetn

        val s_axi_awid     =   Input(UInt(8.W) )       
        val s_axi_awaddr   =   Input(UInt(64.W) )       
        val s_axi_awlen    =   Input(UInt(8.W) )       
        val s_axi_awsize   =   Input(UInt(6.W) )       
        val s_axi_awburst  =   Input(UInt(4.W) )       
        val s_axi_awlock   =   Input(UInt(4.W) )       
        val s_axi_awcache  =   Input(UInt(8.W) )       
        val s_axi_awprot   =   Input(UInt(6.W) )       
        val s_axi_awqos    =   Input(UInt(8.W) )       
        val s_axi_awvalid  =   Input(UInt(2.W)) 
        val s_axi_awready  =   Output(UInt(2.W) ) 

        val s_axi_wid      =   Input(UInt(8.W) )       
        val s_axi_wdata    =   Input(UInt(64.W) )       
        val s_axi_wstrb    =   Input(UInt(8.W) )       
        val s_axi_wlast    =   Input(UInt(2.W) )       
        val s_axi_wvalid   =   Input(UInt(2.W) )       
        val s_axi_wready   =   Output(UInt(2.W) )       
        val s_axi_bid      =   Output(UInt(8.W) )       
        val s_axi_bresp    =   Output(UInt(4.W) )       
        val s_axi_bvalid   =   Output(UInt(2.W) )      

        val s_axi_bready   =   Input(UInt(2.W))        
        val s_axi_arid     =   Input(UInt(8.W))        
        val s_axi_araddr   =   Input(UInt(64.W))        
        val s_axi_arlen    =   Input(UInt(8.W))        
        val s_axi_arsize   =   Input(UInt(6.W))        
        val s_axi_arburst  =   Input(UInt(4.W))        
        val s_axi_arlock   =   Input(UInt(4.W))        
        val s_axi_arcache  =   Input(UInt(8.W))        
        val s_axi_arprot   =   Input(UInt(6.W))       
        val s_axi_arqos    =   Input(UInt(8.W))        
        val s_axi_arvalid  =   Input(UInt(2.W))        
        val s_axi_arready  =   Output(UInt(2.W))        
        val s_axi_rid      =   Output(UInt(8.W))        
        val s_axi_rdata    =   Output(UInt(64.W))       
        val s_axi_rresp    =   Output(UInt(4.W))        
        val s_axi_rlast    =   Output(UInt(2.W))        
        val s_axi_rvalid   =   Output(UInt(2.W))    
        
        val s_axi_rready   =   Input(UInt(2.W))     

        
        val m_axi_awid     =   Output(UInt(4.W))        
        val m_axi_awaddr   =   Output(UInt(32.W) )        
        val m_axi_awlen    =   Output(UInt(4.W) )        
        val m_axi_awsize   =   Output(UInt(3.W) )        
        val m_axi_awburst  =   Output(UInt(2.W) )        
        val m_axi_awlock   =   Output(UInt(2.W) )        
        val m_axi_awcache  =   Output(UInt(4.W) )        
        val m_axi_awprot   =   Output(UInt(3.W) )        
        val m_axi_awqos    =   Output(UInt(4.W) )        
        val m_axi_awvalid  =   Output(UInt(1.W) )        
        
        val m_axi_awready  =   Input(UInt(1.W) )        
        val m_axi_wid      =   Output(UInt(4.W) )        
        val m_axi_wdata    =   Output(UInt(32.W) )        
        val m_axi_wstrb    =   Output(UInt(4.W) )        
        val m_axi_wlast    =   Output(UInt(1.W) )        
        val m_axi_wvalid   =   Output(UInt(1.W) ) 

        val m_axi_wready   =   Input(UInt(1.W) )        
        val m_axi_bid      =   Input(UInt(4.W) )        
        val m_axi_bresp    =   Input(UInt(2.W) )        
        val m_axi_bvalid   =   Input(UInt(1.W))  


        val m_axi_bready   =   Output(UInt(1.W) )        
        val m_axi_arid     =   Output(UInt(4.W) )        
        val m_axi_araddr   =   Output(UInt(32.W) )        
        val m_axi_arlen    =   Output(UInt(4.W) )        
        val m_axi_arsize   =   Output(UInt(3.W) )        
        val m_axi_arburst  =   Output(UInt(2.W) )        
        val m_axi_arlock   =   Output(UInt(2.W) )        
        val m_axi_arcache  =   Output(UInt(4.W) )        
        val m_axi_arprot   =   Output(UInt(3.W) )        
        val m_axi_arqos    =   Output(UInt(4.W) )        
        val m_axi_arvalid  =   Output(UInt(1.W) )        
        val m_axi_rready   =   Output(UInt(1.W))  

        val m_axi_arready  =   Input(UInt(1.W) )        
        val m_axi_rid      =   Input(UInt(4.W) )        
        val m_axi_rdata    =   Input(UInt(32.W) )        
        val m_axi_rresp    =   Input(UInt(2.W))        
        val m_axi_rlast    =   Input(UInt(1.W))         
        val m_axi_rvalid   =   Input(UInt(1.W))       
    
        })

    }


class mycpu_top  extends RawModule with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val         aresetn  = IO(Input(Bool())).suggestName("aresetn")
    val         clk     = IO(Input(Bool())).suggestName("aclk")
    val         ext_int = IO(Input(UInt(6.W)))// 外部中断\
    
    val         arid    = IO(Output(UInt(4.W)))
    val         araddr  = IO(Output(UInt(32.W)))
    val         arlen   = IO(Output(UInt(4.W)))
    val         arsize  = IO(Output(UInt(3.W)))
    val         arburst = IO(Output(UInt(2.W)))
    val         arlock  = IO(Output(UInt(2.W)))
    val         arcache = IO(Output(UInt(4.W)))
    val         arprot  = IO(Output(UInt(3.W)))
    val         arvalid = IO(Output(UInt(1.W)))
    val         arready = IO(Input(UInt(1.W)))
    //rIO()
    val         rid     = IO(Input(UInt(4.W)))
    val         rdata   = IO(Input(UInt(32.W)))
    val         rresp   = IO(Input(UInt(2.W)))
    val         rlast   = IO(Input(UInt(1.W)))
    val         rvalid  = IO(Input(UInt(1.W)))
    val         rready  = IO(Output(UInt(1.W)))
    //awIO(
    val         awid    = IO(Output(UInt(4.W)))
    val         awaddr  = IO(Output(UInt(32.W)))
    val         awlen   = IO(Output(UInt(4.W)))
    val         awsize  = IO(Output(UInt(3.W)))
    val         awburst = IO(Output(UInt(2.W)))
    val         awlock  = IO(Output(UInt(2.W)))
    val         awcache = IO(Output(UInt(4.W)))
    val         awprot  = IO(Output(UInt(3.W)))
    val        awvalid  = IO(Output(UInt(1.W)))
    val        awready  = IO(Input(UInt(1.W)))
    //wIO(
    val        wid      = IO(Output(UInt(4.W)))
    val        wdata    = IO(Output(UInt(32.W)))
    val        wstrb    = IO(Output(UInt(4.W)))
    val        wlast    = IO(Output(UInt(1.W)))
    val        wvalid   = IO(Output(UInt(1.W)))
    val        wready   = IO(Input(UInt(1.W)))
    //bIO(
    val         bid     = IO(Input(UInt(4.W)))
    val         bresp   = IO(Input(UInt(2.W)))
    val         bvalid  = IO(Input(UInt(1.W)))
    val         bready  = IO(Output(UInt(1.W)))

    val   debug_wb_pc       = IO(Output(UInt(32.W)))
    val   debug_wb_rf_wen   = IO(Output(UInt(4.W)))
    val   debug_wb_rf_wnum  = IO(Output(UInt(5.W)))
    val   debug_wb_rf_wdata = IO(Output(UInt(32.W)))

    

withClockAndReset(clk.asClock,(~aresetn).asAsyncReset) {
    val u_axi_cache_bridge = Module(new axi_crossbar_0)
    val u_mips_cpu = Module(new myCPU)
    val icache_first = Module(new inst_cache).io
    val icache = icache_first.port
    val dcache = Module(new data_cache).io.port
    val  arqos = Wire(UInt(4.W))
    val  awqos = Wire(UInt(4.W))

    dcache.sram_addr := u_mips_cpu.data_addr
    dcache.sram_size := u_mips_cpu.data_size
    dcache.sram_cache := u_mips_cpu.data_cache
    dcache.sram_req   := u_mips_cpu.data_req
    dcache.sram_wr    := u_mips_cpu.data_wr
    dcache.sram_wdata := u_mips_cpu.data_wdata
    u_mips_cpu.data_rdata := dcache.sram_rdata
    u_mips_cpu.data_addr_ok := dcache.sram_addr_ok
    u_mips_cpu.data_data_ok := dcache.sram_data_ok

    icache.sram_addr        := u_mips_cpu.inst_addr
    icache.sram_size        := u_mips_cpu.inst_size
    icache.sram_cache       := u_mips_cpu.inst_cache
    icache.sram_req         := u_mips_cpu.inst_req
    icache.sram_wr          := u_mips_cpu.inst_wr
    icache.sram_wdata       := u_mips_cpu.inst_wdata
    u_mips_cpu.inst_rdata   := icache.sram_rdata
    u_mips_cpu.inst_addr_ok := icache.sram_addr_ok
    u_mips_cpu.inst_data_ok := icache.sram_data_ok
    u_mips_cpu.inst_hit  := icache_first.sram_hit

    debug_wb_pc             := u_mips_cpu.debug_wb_pc
    debug_wb_rf_wdata       := u_mips_cpu.debug_wb_rf_wdata
    debug_wb_rf_wen         := u_mips_cpu.debug_wb_rf_wen
    debug_wb_rf_wnum        := u_mips_cpu.debug_wb_rf_wnum
    u_mips_cpu.clk          := clk
    u_mips_cpu.resetn       := aresetn
    u_mips_cpu.ext_int      := ext_int
     

    u_axi_cache_bridge.io.aclk             := clk       
    u_axi_cache_bridge.io.aresetn          := aresetn
    u_axi_cache_bridge.io.s_axi_awid       := Cat(dcache.awid,icache.awid)
    u_axi_cache_bridge.io.s_axi_awaddr     := Cat(dcache.awaddr,icache.awaddr)
    u_axi_cache_bridge.io.s_axi_awlen      := Cat(dcache.awlen,icache.awlen)
    u_axi_cache_bridge.io.s_axi_awsize     := Cat(dcache.awsize,icache.awsize)
    u_axi_cache_bridge.io.s_axi_awburst    := Cat(dcache.awburst,icache.awburst)
    u_axi_cache_bridge.io.s_axi_awlock     := Cat(dcache.awlock,icache.awlock)
    u_axi_cache_bridge.io.s_axi_awcache    := Cat(dcache.awcache,icache.awcache)
    u_axi_cache_bridge.io.s_axi_awprot     := Cat(dcache.awprot,icache.awprot)
    u_axi_cache_bridge.io.s_axi_awqos      := Cat(0.U(4.W),0.U(4.W))
    u_axi_cache_bridge.io.s_axi_awvalid    := Cat(dcache.awvalid,icache.awvalid)
    dcache.awready                          := u_axi_cache_bridge.io.s_axi_awready(1)
    icache.awready                          := u_axi_cache_bridge.io.s_axi_awready(0)

    u_axi_cache_bridge.io.s_axi_wid        := Cat(dcache.wid,icache.wid)
    u_axi_cache_bridge.io.s_axi_wdata      := Cat(dcache.wdata,icache.wdata)
    u_axi_cache_bridge.io.s_axi_wstrb      := Cat(dcache.wstrb,icache.wstrb)
    u_axi_cache_bridge.io.s_axi_wlast      := Cat(dcache.wlast,icache.wlast)
    u_axi_cache_bridge.io.s_axi_wvalid     := Cat(dcache.wvalid,icache.wvalid)
    dcache.wready   := u_axi_cache_bridge.io.s_axi_wready(1)
    icache.wready   := u_axi_cache_bridge.io.s_axi_wready(0)
    dcache.bid      := u_axi_cache_bridge.io.s_axi_bid(7,4)
    icache.bid      := u_axi_cache_bridge.io.s_axi_bid(3,0)
    dcache.bresp    := u_axi_cache_bridge.io.s_axi_bresp(3,2)
    icache.bresp    := u_axi_cache_bridge.io.s_axi_bresp(1,0)
    dcache.bvalid   := u_axi_cache_bridge.io.s_axi_bvalid(1)
    icache.bvalid   := u_axi_cache_bridge.io.s_axi_bvalid(0)
    
    u_axi_cache_bridge.io.s_axi_bready     := Cat(dcache.bready,icache.bready)
    u_axi_cache_bridge.io.s_axi_arid       := Cat(dcache.arid,icache.arid)
    u_axi_cache_bridge.io.s_axi_araddr     := Cat(dcache.araddr,icache.araddr)
    u_axi_cache_bridge.io.s_axi_arlen      := Cat(dcache.arlen,icache.arlen)
    u_axi_cache_bridge.io.s_axi_arsize     := Cat(dcache.arsize,icache.arsize)
    u_axi_cache_bridge.io.s_axi_arburst    := Cat(dcache.arburst,icache.arburst)
    u_axi_cache_bridge.io.s_axi_arlock     := Cat(dcache.arlock,icache.arlock)
    u_axi_cache_bridge.io.s_axi_arcache    := Cat(dcache.arcache,icache.arcache)
    u_axi_cache_bridge.io.s_axi_arprot     := Cat(dcache.arprot,icache.arprot)
    u_axi_cache_bridge.io.s_axi_arqos      := Cat(0.U(4.W),0.U(4.W))
    u_axi_cache_bridge.io.s_axi_arvalid    := Cat(dcache.arvalid,icache.arvalid)
    
    dcache.arready            := u_axi_cache_bridge.io.s_axi_arready(1)
    icache.arready            := u_axi_cache_bridge.io.s_axi_arready(0)
    dcache.rid                := u_axi_cache_bridge.io.s_axi_rid(7,4)
    icache.rid                := u_axi_cache_bridge.io.s_axi_rid(3,0)
    dcache.rdata              := u_axi_cache_bridge.io.s_axi_rdata(63,32)
    icache.rdata              := u_axi_cache_bridge.io.s_axi_rdata(31,0)
    dcache.rresp              := u_axi_cache_bridge.io.s_axi_rresp(3,2)
    icache.rresp              := u_axi_cache_bridge.io.s_axi_rresp(1,0)
    dcache.rlast              := u_axi_cache_bridge.io.s_axi_rlast(1)
    icache.rlast              := u_axi_cache_bridge.io.s_axi_rlast(0)
    dcache.rvalid             := u_axi_cache_bridge.io.s_axi_rvalid(1)
    icache.rvalid             := u_axi_cache_bridge.io.s_axi_rvalid(0)

    u_axi_cache_bridge.io.s_axi_rready    := Cat(dcache.rready,icache.rready)


    awid                := u_axi_cache_bridge.io.m_axi_awid       
    awaddr              := u_axi_cache_bridge.io.m_axi_awaddr     
    awlen               := u_axi_cache_bridge.io.m_axi_awlen      
    awsize              := u_axi_cache_bridge.io.m_axi_awsize     
    awburst             := u_axi_cache_bridge.io.m_axi_awburst    
    awlock              :=u_axi_cache_bridge.io.m_axi_awlock     
    awcache             :=u_axi_cache_bridge.io.m_axi_awcache    
    awprot              :=u_axi_cache_bridge.io.m_axi_awprot     
    awqos               :=u_axi_cache_bridge.io.m_axi_awqos      
    awvalid             :=u_axi_cache_bridge.io.m_axi_awvalid    
   
    u_axi_cache_bridge.io.m_axi_awready     :=   awready
    wid                                     :=u_axi_cache_bridge.io.m_axi_wid        
    wdata                                   :=u_axi_cache_bridge.io.m_axi_wdata      
    wstrb                                   :=u_axi_cache_bridge.io.m_axi_wstrb      
    wlast                                   :=u_axi_cache_bridge.io.m_axi_wlast      
    wvalid                                  :=u_axi_cache_bridge.io.m_axi_wvalid     
    u_axi_cache_bridge.io.m_axi_wready      := wready
    u_axi_cache_bridge.io.m_axi_bid         :=bid
    u_axi_cache_bridge.io.m_axi_bresp       := bresp
    u_axi_cache_bridge.io.m_axi_bvalid      :=bvalid
    bready              :=u_axi_cache_bridge.io.m_axi_bready     

    arid                :=u_axi_cache_bridge.io.m_axi_arid       
    araddr              :=u_axi_cache_bridge.io.m_axi_araddr     
    arlen               :=u_axi_cache_bridge.io.m_axi_arlen      
    arsize              :=u_axi_cache_bridge.io.m_axi_arsize     
    arburst             :=u_axi_cache_bridge.io.m_axi_arburst    
    arlock              :=u_axi_cache_bridge.io.m_axi_arlock     
    arcache             :=u_axi_cache_bridge.io.m_axi_arcache    
    arprot              :=u_axi_cache_bridge.io.m_axi_arprot     
    arqos               :=u_axi_cache_bridge.io.m_axi_arqos      
    arvalid             :=u_axi_cache_bridge.io.m_axi_arvalid    
    
    u_axi_cache_bridge.io.m_axi_arready   := arready 
    u_axi_cache_bridge.io.m_axi_rid    :=    rid 
    u_axi_cache_bridge.io.m_axi_rdata  :=    rdata
    u_axi_cache_bridge.io.m_axi_rresp  :=    rresp
    u_axi_cache_bridge.io.m_axi_rlast  :=    rlast
    u_axi_cache_bridge.io.m_axi_rvalid :=    rvalid
    rready              := u_axi_cache_bridge.io.m_axi_rready     


}}

object my_CPU_top_test extends App{
    (new ChiselStage).emitVerilog(new mycpu_top)
}
