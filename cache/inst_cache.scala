package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
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

        
        val     sram_cache = Input(UInt(1.W))
}

class inst_cache  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
        val port = new axi_ram_port
        val     sram_hit    = Output(UInt(1.W))
        // // axi
        // // ar
        // val  arid    = Output(UInt(4.W))
        // val  araddr  = Output(UInt(32.W))
        // val  arlen   = Output(UInt(8.W))
        // val  arsize  = Output(UInt(3.W))
        // val  arburst = Output(UInt(2.W))
        // val  arlock  = Output(UInt(2.W))
        // val  arcache = Output(UInt(4.W))
        // val  arprot  = Output(UInt(3.W))
        // val  arvalid = Output(UInt(1.W))
        // val  arready = Input(UInt(1.W))
        // //r
        // val         rid    = Input(UInt(4.W))
        // val         rdata  = Input(UInt(32.W))
        // val         rresp  = Input(UInt(2.W))
        // val         rlast  = Input(UInt(1.W))
        // val         rvalid = Input(UInt(1.W))
        // val         rready = Output(UInt(1.W))
        // //aw
        // val         awid    = Output(UInt(4.W))
        // val         awaddr  = Output(UInt(32.W))
        // val         awlen   = Output(UInt(8.W))
        // val         awsize  = Output(UInt(3.W))
        // val         awburst = Output(UInt(2.W))
        // val         awlock  = Output(UInt(2.W))
        // val         awcache = Output(UInt(4.W))
        // val         awprot  = Output(UInt(3.W))
        // val        awvalid  = Output(UInt(1.W))
        // val        awready  = Input(UInt(1.W))
        // //w
        // val        wid      = Output(UInt(4.W))
        // val        wdata    = Output(UInt(32.W))
        // val        wstrb    = Output(UInt(4.W))
        // val        wlast    = Output(UInt(1.W))
        // val        wvalid   = Output(UInt(1.W))
        // val        wready   = Input(UInt(1.W))
        // //b
        // val         bid     = Input(UInt(4.W))
        // val         bresp   = Input(UInt(2.W))
        // val         bvalid  = Input(UInt(1.W))
        // val         bready  = Output(UInt(1.W))
        
        // // from cpu  sram like
        // val     sram_req =  Input(UInt(1.W))
        // val     sram_wr =   Input(UInt(1.W))
        // val     sram_size = Input(UInt(2.W))
        // val     sram_addr = Input(UInt(32.W))
        // val     sram_wdata= Input(UInt(32.W))
        // val     sram_addr_ok = Output(UInt(1.W))
        // val     sram_data_ok = Output(UInt(1.W))
        // val     sram_rdata =   Output(UInt(32.W))
        
        // val     sram_cache = Input(UInt(1.W))

    })
    val lru = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
    val sram_addr_reg = RegInit(0.U(32.W))
    val sram_cache_reg = RegInit(0.U(1.W))

    val access_cache_addr =  Mux(io.port.sram_req.asBool,io.port.sram_addr,sram_addr_reg)
    val access_cache_state =  Mux(io.port.sram_req.asBool,io.port.sram_cache,sram_cache_reg)

    // val write_counter_same = RegInit(0.U(1.W))
    

    sram_addr_reg := Mux(io.port.sram_req.asBool,io.port.sram_addr,sram_addr_reg)
    sram_cache_reg := Mux(io.port.sram_req.asBool,io.port.sram_cache,sram_cache_reg)


    val icache_tag_0     = Module(new icache_tag).io
    val icache_tag_1     = Module(new icache_tag).io
    icache_tag_0.op     := 0.U
    icache_tag_1.op     := 0.U
    icache_tag_0.addr   := access_cache_addr
    icache_tag_1.addr   := access_cache_addr

    val icache_data_way0 =  VecInit(Seq.fill(8)(Module(new icache_data).io))
    val icache_data_way1 =  VecInit(Seq.fill(8)(Module(new icache_data).io))
    // icache_data_way0(0).
    val state_reset = 0.U
    val state_lookup = 1.U
    val state_access_ram_0 = "b0010".U
    val state_access_ram_1 = "b0011".U
    val state_data_ready   = "b0100".U
    val state_miss_access_ram_0 = "b0101".U
    val state_miss_access_ram_1 = "b0110".U
    val state_miss_update       = "b0111".U

    val work_state = RegInit(0.U(4.W))
    val write_counter  = RegInit(0.U(3.W))
    val wait_data  = RegInit(0.U(32.W))
    val hit_reg = RegInit(0.U(1.W))
    
    lru(sram_addr_reg(11,5)) := Mux(work_state === state_lookup,
        Mux(icache_tag_0.hit.asBool /*&& icache_tag_0.valid.asBool*/,1.U.asBool,
        Mux(icache_tag_1.hit.asBool /*&& icache_tag_1.valid.asBool*/,0.U.asBool,lru(sram_addr_reg(11,5)) )),
        Mux(work_state === state_miss_update,~lru(sram_addr_reg(11,5)),  lru(sram_addr_reg(11,5)) ))
    
    val hit = (icache_tag_0.hit.asBool && icache_tag_0.valid.asBool) || 
        (icache_tag_1.hit.asBool && icache_tag_1.valid.asBool)
    val hit0_Reg = RegInit(0.U(1.W))
    val hit1_Reg = RegInit(0.U(1.W))
    hit0_Reg := icache_tag_0.hit.asBool && icache_tag_0.valid.asBool
    hit1_Reg := icache_tag_1.hit.asBool && icache_tag_1.valid.asBool
    
    work_state := Mux(work_state === state_reset && io.port.sram_req.asBool,Mux(io.port.sram_cache.asBool,state_lookup,state_access_ram_0),
        Mux(work_state===state_access_ram_0 && io.port.arready.asBool,state_access_ram_1,
        Mux(work_state===state_access_ram_1 && io.port.rvalid.asBool,state_data_ready,
        Mux(work_state===state_data_ready,Mux(io.port.sram_req.asBool,Mux(io.port.sram_cache.asBool,state_lookup,state_access_ram_0),state_reset),
        Mux(work_state===state_lookup,Mux(hit,Mux(io.port.sram_req.asBool,Mux(io.port.sram_cache.asBool,state_lookup,state_access_ram_0),state_reset),
           Mux(access_cache_state.asBool,state_miss_access_ram_0,state_access_ram_0)),
        Mux(work_state===state_miss_access_ram_0 && io.port.arready.asBool,state_miss_access_ram_1,
        Mux(work_state===state_miss_access_ram_1 ,Mux(io.port.rlast.asBool && io.port.rvalid.asBool,state_miss_update,work_state),
        Mux(work_state===state_miss_update,state_data_ready,work_state))))))))//咱们复位都是在一个时钟周期内复位的

    wait_data := Mux(work_state === state_access_ram_1 && io.port.rvalid.asBool,io.port.rdata,
         Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool && write_counter === sram_addr_reg(4,2),io.port.rdata,wait_data))
    
    write_counter := Mux(work_state === state_miss_access_ram_1,Mux(io.port.rvalid.asBool && io.port.rlast.asBool,0.U,Mux(io.port.rvalid.asBool,write_counter+1.U,write_counter)),write_counter)
    // val write_counter_same = write_counter === sram_addr_reg(4,2) && work_state === state_miss_access_ram_1 && io.port.rvalid.asBool && hit

    val word_selection0 = icache_data_way0(sram_addr_reg(4,2)).rdata
    val word_selection1 = icache_data_way1(sram_addr_reg(4,2)).rdata
   
    for(i <- 0 to 7 ) {
        icache_data_way0(i).addr := access_cache_addr
        icache_data_way0(i).wdata := io.port.rdata
        icache_data_way0(i).en := 1.U
    }
    for(i <- 0 to 7 ) {
        icache_data_way1(i).addr := access_cache_addr
        icache_data_way1(i).wdata := io.port.rdata
        icache_data_way1(i).en := 1.U
    }
    for(i <- 0 to 7 ) {icache_data_way0(i).wen  := Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool
        && lru(sram_addr_reg(11,5)) === 0.U && write_counter === i.asUInt,"b1111".U,0.U) }
    for(i <- 0 to 7 ) {icache_data_way1(i).wen  := Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool
        && lru(sram_addr_reg(11,5)) === 1.U && write_counter === i.asUInt,"b1111".U,0.U) }
    val hit_word = Mux(hit0_Reg.asBool,word_selection0,Mux(hit1_Reg.asBool,word_selection1,0.U))
    
    icache_tag_0.wen := Mux(work_state === state_miss_update && lru(sram_addr_reg(11,5)) === 0.U,1.U,0.U)
    icache_tag_1.wen := Mux(work_state === state_miss_update  && lru(sram_addr_reg(11,5)) === 1.U,1.U,0.U)
    icache_tag_0.wdata := Mux(work_state === state_miss_update  ,Cat(1.U(1.W),sram_addr_reg(31,12)),0.U)
    icache_tag_1.wdata := Mux(work_state === state_miss_update  ,Cat(1.U(1.W),sram_addr_reg(31,12)),0.U)

       
       
    //axi signal
    io.port.arid := 0.U
    io.port.araddr := Mux(work_state === state_access_ram_0,sram_addr_reg,
        Mux(work_state === state_miss_access_ram_0,Cat(sram_addr_reg(31,5),0.U(5.W)),0.U))
    io.port.arlen  := Mux(sram_cache_reg.asBool,"b111".U,0.U)
    io.port.arsize := "b010".U
    io.port.arburst := Mux(sram_cache_reg.asBool,1.U,0.U)
    io.port.arlock  := 0.U
    io.port.arcache := 0.U
    io.port.arprot  := 0.U
    io.port.arvalid := (work_state === state_access_ram_0 || work_state === state_miss_access_ram_0)
    io.port.rready  := 1.U

    io.port.sram_addr_ok := 1.U
    io.port.sram_data_ok := Mux(work_state === state_data_ready,1.U,0.U)//|write_counter_same
    io.port.sram_rdata := Mux(work_state === state_data_ready ,wait_data,Mux(work_state === state_lookup,hit_word,
        /*Mux(work_state === state_miss_access_ram_1 && write_counter_same ,io.port.rdata,*/0.U))
    hit_reg := Mux(io.port.sram_req.asBool,hit,0.U)
    
    io.sram_hit  := hit_reg
    // io.sram_hit :=  Mux(io.port.sram_req.asBool,hit,0.U)

       // don't care
    io.port.awid    := "b0000".U
    io.port.awaddr  := "b0".U
    io.port.awlen   := "b0".U
    io.port.awsize  := "b010".U
    io.port.awburst := "b00".U
    io.port.awlock  := "b00".U
    io.port.awcache := "b0000".U
    io.port.awprot  := "b000".U
    io.port.awvalid := "b0".U

    io.port.wid    := "b0000".U
    io.port.wdata  := "b0".U
    io.port.wstrb  := "b0000".U
    io.port.wlast  := "b0".U
    io.port.wvalid := "b0".U

    io.port.bready := 0.U

}
object inst_cache_test extends App{
    (new ChiselStage).emitVerilog(new inst_cache)
}


