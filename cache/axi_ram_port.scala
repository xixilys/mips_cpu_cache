package examples

import chisel3._
import chisel3.stage._
import chisel3.util._

//multi-banking-cache 用单端口sram模拟双端口cache
class axi_ram_port_multi_banking extends Bundle {
     // axi
        // ar
        val         arid    = Output(UInt(4.W))
        val         araddr  = Output(UInt(32.W))
        val         arlen   = Output(UInt(4.W))
        val         arsize  = Output(UInt(3.W))
        val         arburst = Output(UInt(2.W))
        val         arlock  = Output(UInt(2.W))
        val         arcache = Output(UInt(4.W))
        val         arprot  = Output(UInt(3.W))
        val         arvalid = Output(UInt(1.W))
        val         arready = Input(UInt(1.W))
        //r
        val         rid     = Input(UInt(4.W))
        val         rdata   = Input(UInt(32.W))
        val         rresp   = Input(UInt(2.W))
        val         rlast   = Input(UInt(1.W))
        val         rvalid  = Input(UInt(1.W))
        val         rready  = Output(UInt(1.W))
        //aw
        val         awid    = Output(UInt(4.W))
        val         awaddr  = Output(UInt(32.W))
        val         awlen   = Output(UInt(4.W))
        val         awsize  = Output(UInt(3.W))
        val         awburst = Output(UInt(2.W))
        val         awlock  = Output(UInt(2.W))
        val         awcache = Output(UInt(4.W))
        val         awprot  = Output(UInt(3.W))
        val        awvalid  = Output(UInt(1.W))
        val        awready  = Input(UInt(1.W))
        //w
        val        wid      = Output(UInt(4.W))
        val        wdata    = Output(UInt(32.W))
        val        wstrb    = Output(UInt(4.W))
        val        wlast    = Output(UInt(1.W))
        val        wvalid   = Output(UInt(1.W))
        val        wready   = Input(UInt(1.W))
        //b
        val         bid     = Input(UInt(4.W))
        val         bresp   = Input(UInt(2.W))
        val         bvalid  = Input(UInt(1.W))
        val         bready  = Output(UInt(1.W))
        
        // from cpu  sram like
        val     sram_req    = Input(UInt(1.W))
        val     sram_wr     = Input(UInt(1.W))
        val     sram_size   = Input(UInt(2.W))
        val     sram_addr   = Input(UInt(32.W))
        val     sram_wdata  = Input(UInt(32.W))
        val     sram_addr_ok= Output(UInt(1.W))
        val     sram_data_ok= Output(UInt(1.W))
        val     sram_data_valid = Output(UInt(3.W))
        val     sram_write_en  = Output(UInt(2.W))//有多少位数据是有效的

        val     sram_rdata_L  = Output(UInt(32.W))
        val     sram_rdata_M  = Output(UInt(32.W))
        val     sram_rdata_R  = Output(UInt(32.W))

        val     sram_cache = Input(UInt(1.W))
        
        
}
class axi_ram_port extends Bundle {
     // axi
        // ar
        val         arid    = Output(UInt(4.W))
        val         araddr  = Output(UInt(32.W))
        val         arlen   = Output(UInt(4.W))
        val         arsize  = Output(UInt(3.W))
        val         arburst = Output(UInt(2.W))
        val         arlock  = Output(UInt(2.W))
        val         arcache = Output(UInt(4.W))
        val         arprot  = Output(UInt(3.W))
        val         arvalid = Output(UInt(1.W))
        val         arready = Input(UInt(1.W))
        //r
        val         rid     = Input(UInt(4.W))
        val         rdata   = Input(UInt(32.W))
        val         rresp   = Input(UInt(2.W))
        val         rlast   = Input(UInt(1.W))
        val         rvalid  = Input(UInt(1.W))
        val         rready  = Output(UInt(1.W))
        //aw
        val         awid    = Output(UInt(4.W))
        val         awaddr  = Output(UInt(32.W))
        val         awlen   = Output(UInt(4.W))
        val         awsize  = Output(UInt(3.W))
        val         awburst = Output(UInt(2.W))
        val         awlock  = Output(UInt(2.W))
        val         awcache = Output(UInt(4.W))
        val         awprot  = Output(UInt(3.W))
        val        awvalid  = Output(UInt(1.W))
        val        awready  = Input(UInt(1.W))
        //w
        val        wid      = Output(UInt(4.W))
        val        wdata    = Output(UInt(32.W))
        val        wstrb    = Output(UInt(4.W))
        val        wlast    = Output(UInt(1.W))
        val        wvalid   = Output(UInt(1.W))
        val        wready   = Input(UInt(1.W))
        //b
        val         bid     = Input(UInt(4.W))
        val         bresp   = Input(UInt(2.W))
        val         bvalid  = Input(UInt(1.W))
        val         bready  = Output(UInt(1.W))
        
        // from cpu  sram like
        val     sram_req    = Input(UInt(1.W))
        val     sram_wr     = Input(UInt(1.W))
        val     sram_size   = Input(UInt(2.W))
        val     sram_addr   = Input(UInt(32.W))
        val     sram_wdata  = Input(UInt(32.W))
        val     sram_addr_ok= Output(UInt(1.W))
        val     sram_data_ok= Output(UInt(1.W))
        val     sram_rdata  = Output(UInt(32.W))
        // val     sram_data_valid = Output(UInt(3.W))

        // val     sram_rdata_L  = Output(UInt(32.W))
        // val     sram_rdata_M  = Output(UInt(32.W))
        // val     sram_rdata_R  = Output(UInt(32.W))

        val     sram_cache = Input(UInt(1.W))
        
        
}
