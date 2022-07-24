package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import os.makeDir
import mips_cpu_2nd.inst_cache_2nd
/*
目前已经禁用了swl和lwl相关的功能功能
若想启用，修改  Mem_withRL_Data := _dmem.io.RD
修改mem_write



*/

class inst_port extends Bundle {
    val   inst_req = Output(UInt(1.W))
    val   inst_wr = Output(UInt(1.W))
    val   inst_size = Output(UInt(2.W))
    val   inst_addr = Output(UInt(32.W))
    val   inst_wdata = Output(UInt(32.W))
    // val   inst_cache = Output(UInt(1.W))
    val   inst_addr_ok = Input(UInt(1.W))
    val   inst_data_ok = Input(UInt(1.W))
    val   inst_rdata = Input(UInt(32.W))
    val   inst_hit = Input(UInt(1.W))
}
class myCPU extends RawModule with mips_macros {//
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜    
    val   ext_int = IO(Input(UInt(6.W)))
    val   resetn  = IO(Input(Bool())).suggestName("resetn")
    val   clk    = IO(Input(Bool())).suggestName("clk")
    
    val   inst_cache = IO(Output(UInt(1.W)))
    val   inst_req = IO(Output(UInt(1.W))).suggestName("inst_sram_en")
    val   inst_wr = IO(Output(UInt(1.W))).suggestName("inst_sram_wen")
    val   inst_size = IO(Output(UInt(2.W)))
    val   inst_addr = IO(Output(UInt(32.W))).suggestName("inst_sram_addr")
    val   inst_wdata = IO(Output(UInt(32.W))).suggestName("inst_sram_wdata")
    val   inst_addr_ok = IO(Input(UInt(1.W)))
    val   inst_data_ok = IO(Input(UInt(1.W)))
    val   inst_rdata_L = IO(Input(UInt(32.W))).suggestName("inst_sram_rdata_L") 
    val   inst_rdata_M = IO(Input(UInt(32.W))).suggestName("inst_sram_rdata_M") 
    val   inst_rdata_R = IO(Input(UInt(32.W))).suggestName("inst_sram_rdata_R") 

    //val   inst_cache_working_on = IO(Input(Bool())).suggestName("cache_working_on")   
    //val   inst_rdata = IO(Input(UInt(32.W))).suggestName("inst_sram_rdata") 
    val   inst_hit = IO(Input(UInt(1.W))).suggestName("inst_sram_hit")
    val   inst_valid = IO(Input(UInt(3.W))).suggestName("inst_data_valid")//代表回来的数据哪一位有效
    val   inst_write_en = IO(Input(UInt(2.W))).suggestName("inst_write_en")//代表回来的数据哪一位有效

    val   stage2_flush = IO(Output(Bool()))
    val   stage2_stall = IO(Input(Bool()))
    val   stage1_valid_flush = IO(Output(UInt(2.W)))
    val   inst_ready_to_use = IO(Output(Bool()))

    val   inst_buffer_full = IO(Output(Bool()))

    val   data_req = IO(Output(UInt(1.W))).suggestName("data_sram_en")
    val   data_wr = IO(Output(UInt(1.W))).suggestName("data_sram_wen")
    val   data_size = IO(Output(UInt(2.W)))
    val   data_addr = IO(Output(UInt(32.W))).suggestName("data_sram_addr")
    val   data_wdata = IO(Output(UInt(32.W))).suggestName("data_sram_wdata")
    val   data_cache = IO(Output(UInt(1.W)))
    val   data_addr_ok = IO(Input(UInt(1.W)))
    val   data_data_ok = IO(Input(UInt(1.W)))
    val   data_rdata = IO(Input(UInt(32.W))).suggestName("data_sram_rdata") 
   // val   data_hit = IO(Input(UInt(1.W)))
   
    val   debug_wb_pc       = IO(Output(UInt(32.W)))
    val   debug_wb_rf_wen   = IO(Output(UInt(4.W)))
    val   debug_wb_rf_wnum  = IO(Output(UInt(5.W)))
    val   debug_wb_rf_wdata = IO(Output(UInt(32.W)))
        

    override def desiredName = "myCPU"
withClockAndReset(clk.asClock,(~resetn).asAsyncReset) {


// ------实例化所有模块,并且连接部分线
    val _alu = Module(new alu)
    val _br  = Module(new br)
    val _cfu = Module(new cfu)
    val _cp0 = Module(new cp0)
    val _cu  = Module(new cu)
    val _dmem = Module(new dmem)
    val _dmemreq = Module(new dmemreq)
    val _ex2mem  = Module(new ex2mem)
    val _hilo    = Module(new hilo)
    val _id2ex   = Module(new id2ex)
    val _if2id   = Module(new if2id(5,2))
    // val _imem    = Module(new imem)
    val _mem2wb  = Module(new mem2wb)
    val _mmu     = Module(new mmu)
    val _muldiv  = Module(new muldiv)
    // val _pc2if   = Module(new pc2if)
    val _regfile = Module(new regfile)

    // io.inst_port_test <> _pc2if.io.inst_port_test
    // inst_port_test <> _pc2if.io.inst_port_test
//    _if2id.io <> _pc2if.io
   _cu.io    <> _id2ex.io1
   _id2ex.io2 <> _ex2mem.io1

// -----------定义所有相关的变量-------
//-------------PC----------
    inst_wr   := 0.U//_pc2if.io.inst_port_test.inst_wr
//-------------fec_1 其实就是在cache 中强行增加一个模块----------
    

val stage_fec_1_pc = Wire(UInt(32.W))

//-------------IF----------

    //val ExceptionTypeF = Mux(_pc2if.io.InstUnalignedF.asBool,EXCEP_MASK_AdELI,0.U)//3）取指PC不对齐于字边界，指令不对齐例外,由mmu处获得
   
    //-------------ID----------
    val ExceptionTypeD_Out = Wire(UInt(32.W))
    _id2ex.io.en            :=  _cfu.io.StallD
    _id2ex.io.clr           :=  _cfu.io.FlushE
    val InstrD = _if2id.io.InstrD
    val BranchRsD = Wire(UInt(32.W))//Mux(_cfu.io.ForwardAD.asBool,ResultM,_regfile.io.RD1)//前递，降低流水线阻塞,后面是指需要读寄存器的时候的值
    val BranchRtD = Wire(UInt(32.W))
    val PCPLus4D = _if2id.io.PCD + 4.U
    val PCPLus8D = _if2id.io.PCD + 8.U 
    val PCSrcD  = Cat((_cu.io.JumpD.asBool && _cfu.io.StallD.asBool),_br.io.exe.asBool && _cfu.io.StallD.asBool)//Wire(UInt(2.W)) 要不要跳转 + 有没有分支指令需要跳转
    val PCBranchD = Cat(Cat(Seq.fill(14)(_if2id.io.InstrD(15))),_if2id.io.InstrD(15,0),0.U(2.W)) + PCPLus4D//对指令进行有符号扩展之后的东西
    val PCJumpD   = Mux(_cu.io.JRD.asBool,BranchRsD,Cat(PCPLus4D(31,28),InstrD(25,0),0.U(2.W)) )// 分支指令均需要读寄存器并且进行比较,由于我是在解码阶段即判断到底需不需要跳转，所以需要用id 阶段的pc进行运算
    val RsD       = InstrD(25,21)
    val RtD       = InstrD(20,16)
    val RdD       = InstrD(15,11)
    val ImmD      = Mux(_cu.io.ImmUnsigned.asBool, unsign_extend(InstrD(15,0),16),sign_extend(InstrD(15,0),16))
    val Write_WriteCP0AddrW = InstrD(15,11)
    val Write_WriteCP0Sel0  = InstrD(2,0)


//-------------EX----------
    _ex2mem.io.en            :=  _cfu.io.StallE
    _ex2mem.io.clr           :=  _cfu.io.FlushM
    val RD1ForWardE = Wire(UInt(32.W))
    val RD2ForWardE = Wire(UInt(32.W))
    val WriteDataE = RD2ForWardE
    val BadVAddrE  = Wire(UInt(32.W))
    val RtM = _ex2mem.io.RtM
    val ReadData_dmem = data_rdata


    val mem_write_data_rl = Wire(UInt(32.W))
    val mem_cached = RegInit(0.U(1.W))
    mem_cached := Mux(data_req.asBool,data_cache,mem_cached)
    val mem_write = 0.U.asBool//(_ex2mem.io.MemRLM =/= 0.U && _ex2mem.io.MemWriteM.asBool) 

    data_req := ((_dmemreq.io.req.asBool || mem_write )&& !_dmem.io.data_pending)

    data_addr := Mux(mem_write,Cat(_ex2mem.io.PhyAddrM(31,2),0.U(2.W)),_dmemreq.io.addr)
    data_wdata := Mux(mem_write,mem_write_data_rl,_dmemreq.io.wdata)

    data_wr := Mux(_id2ex.io2.MemRLE =/= 0.U,0x0.U,Mux(mem_write,0x1.U,_dmemreq.io.wr))//无论是写的还是读的LR，在exe阶段都得取数据
    mem_write_data_rl :=  Mux(_ex2mem.io.MemRLM === "b10".U,
        MuxLookup(_ex2mem.io.PhyAddrM(1,0),RtM,Seq(
            0.U -> Cat(data_rdata(31,8),RtM(31,24)),
            1.U -> Cat(data_rdata(31,16),RtM(31,16)),
            2.U -> Cat(data_rdata(31,24),RtM(31,8))
        )),MuxLookup(_ex2mem.io.PhyAddrM(1,0),_ex2mem.io.RtM,Seq(
            1.U -> Cat(RtM(23,0),ReadData_dmem(7,0)),
            2.U -> Cat(RtM(15,0),ReadData_dmem(15,0)),
            3.U -> Cat(RtM(7,0),ReadData_dmem(23,0))
        ))) 
    
//-------------MEM----------
    _mem2wb.io.en            :=  _cfu.io.StallM
    _mem2wb.io.clr           := _cfu.io.FlushW
    _dmem.io.req        := _dmemreq.io.req
    _dmem.io.addr_ok    := data_addr_ok
    _dmem.io.data_ok    := data_data_ok
    _dmem.io.rdata      := data_rdata
    _dmem.io.ReadEn     := _ex2mem.io.MemToRegM
    _dmem.io.WIDTH      := _ex2mem.io.MemWidthM
    _dmem.io.SIGN := !_ex2mem.io.LoadUnsignedM.asBool
    // _dmem.io.WE   :=  _ex2mem.io.MemWriteM
    // _dmem.io.WIDTH := _ex2mem.io.MemWidthM
    // _dmem.io.Physisc_Address := _ex2mem.io.PhyAddrM
    // _dmem.io.SIGN       := _ex2mem.io.LoadUnsignedM
    _dmem.io.Physisc_Address := _ex2mem.io.PhyAddrM

    val ResultM = Wire(UInt(32.W))
    val HiInM   = _ex2mem.io.HiInM
    val LoInM   = _ex2mem.io.LoInM
    val PCPlus8M = _ex2mem.io.PCPlus8M
    val Forward_ResultM = Wire(UInt(32.W))
    _mem2wb.io.RegWriteM := _ex2mem.io.RegWriteM
    _mem2wb.io.MemToRegM            := _ex2mem.io.MemToRegM
    _mem2wb.io.WriteRegM            := _ex2mem.io.WriteRegM
    _mem2wb.io.HiInM                := _ex2mem.io.HiInM
    _mem2wb.io.LoInM                := _ex2mem.io.LoInM
    _mem2wb.io.InDelaySlotM         := _ex2mem.io.InDelaySlotM

    _mem2wb.io.PCM                  := _ex2mem.io.PCM
    _mem2wb.io.CP0WriteM            := _ex2mem.io.CP0WriteM
    _mem2wb.io.WriteCP0AddrM        := _ex2mem.io.WriteCP0AddrM 
    _mem2wb.io.WriteCP0SelM         := _ex2mem.io.WriteCP0SelM
    _mem2wb.io.WriteCP0HiLoDataM    := _ex2mem.io.WriteCP0HiLoDataM
    _mem2wb.io.HiLoWriteM           := _ex2mem.io.HiLoWriteM 

//-------------WB----------

    val HiInW     =  _mem2wb.io.HiInW
    val LoInW     =  _mem2wb.io.LoInW//
    val ResultW   =  Wire(UInt(32.W))
    val RegWriteW =  Wire(UInt(1.W))

    val PCW_Reg = RegInit(0.U(32.W))
    val slot_Reg = RegInit(0.U(1.W))
    val branchjump_Jr_Reg = RegInit(0.U(2.W))
    val Exception_state_Reg = RegInit(0.U(1.W))
    PCW_Reg := Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.PCW,PCW_Reg)
    slot_Reg := Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.InDelaySlotW,slot_Reg)
    branchjump_Jr_Reg := Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.BranchJump_JrW,branchjump_Jr_Reg)
    Exception_state_Reg := Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.ExceptionTypeW_Out =/= 0.U,Exception_state_Reg )
    val Exception_state = Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.ExceptionTypeW_Out =/= 0.U,Exception_state_Reg)
    // val wb_expction_type
    debug_wb_pc := _mem2wb.io.PCW
    debug_wb_rf_wen := Mux(RegWriteW.asBool,0xf.U,0.U)
    debug_wb_rf_wnum := _regfile.io.A3
    debug_wb_rf_wdata := _regfile.io.WD3
//pc有关的一些可以复用的模块
class pc_in_bundle extends Bundle {
    val pc_value_in = Input(UInt(32.W))
    val pc_inst_in = Input(UInt(32.W))
    // val pc_valid_In = Input(Bool())
}
class pc_out_bundle extends Bundle {
    val pc_value_out = Output(UInt(32.W))
    val pc_inst_out  = Output(UInt(32.W))
    // val pc_valid_Out = Output(Bool())
}
class pc_detail extends Module {
    val io = IO(new Bundle{
        val stall = Input(Bool())
        val flush = Input(Bool())   
    })
    val io_in = IO(new pc_in_bundle)
    val io_out = IO(new pc_out_bundle)
      val pc_value = Reg(UInt(32.W))//RegInit(Cat(0xbfbf.U,0xfffc.U))
    pc_value := Mux(reset.asBool,Cat(0xbfbf.U,0xfff4.U),Mux(io.flush,0.U,Mux(io.stall,io_in.pc_value_in,pc_value)))
    io_out.pc_value_out := pc_value
    val pc_inst = RegInit(0.U(32.W))
    pc_inst  := Mux(io.flush,0.U,Mux(io.stall,io_in.pc_inst_in,pc_inst))
    io_out.pc_inst_out  := pc_inst
}
//cfu
    val _pre_cfu = Module(new pre_cfu)
 // ---------- PC_cal ----------
    //_pc2if.io <> io//sram like增加AXI总线接口，仅仅增加了一个握手
   
    //pc_next 不在這裡算
    val stage_fec_1_pc_next = Wire(UInt(32.W))
    val Pc_Next = Wire(UInt(32.W))
    val Pc_Next_normal = Mux2_4(PCSrcD,stage_fec_1_pc_next,PCBranchD,PCJumpD ,0.U)
    val pc_next_wait = RegInit(0.U(32.W))
    _mmu.io.i_vaddr := Pc_Next
    val ready_to_branch = Wire(Bool())
    pc_next_wait := Mux(ready_to_branch,Pc_Next,pc_next_wait)
    val pc_req_wait = RegInit(0.U.asBool)
    when(!inst_req.asBool && ready_to_branch ) {
        pc_req_wait := 1.U
    }.elsewhen(pc_req_wait && inst_req.asBool) {
        pc_req_wait := 0.U
    }.otherwise{
        pc_req_wait := pc_req_wait
    }


    val exception_Pc_reg = RegInit(0.U(32.W))
    exception_Pc_reg := Mux(_cp0.io.exception.asBool,_cp0.io.return_pc,exception_Pc_reg)
    val ready_to_returnPc = _cp0.io.exception.asBool
    val returnPc_req_wait = RegInit(0.U.asBool)
    //val access_returnPc_req_wait = Wire(Bool())

    when(!inst_req.asBool  && ready_to_returnPc.asBool ) {
        returnPc_req_wait := 1.U
    }.elsewhen(returnPc_req_wait && inst_req.asBool) {
        returnPc_req_wait := 0.U
    }.otherwise{
        returnPc_req_wait := returnPc_req_wait
    }

    val return_pc_wait = RegInit(0.U.asBool)
    


    Pc_Next := Mux(_cp0.io.exception.asBool,_cp0.io.return_pc,Mux(returnPc_req_wait,exception_Pc_reg,Mux(pc_req_wait,pc_next_wait,Pc_Next_normal)))





    val pc_fetch = Wire(UInt(32.W)) // 这玩意是最后真的扔到cache中的指针
    pc_fetch := Pc_Next
    inst_cache  := _mmu.io.i_cached
    inst_req    := stage2_stall//_pre_cfu.io.stage_pc_cal_stall
    inst_ready_to_use := !_mmu.io.i_unaligned  
    inst_addr   := _mmu.io.i_paddr

    inst_size := 2.U // 一直都是32位，也就是2.U
    inst_wdata := 0.U //写请求的写数据，咱们只需要读数据即可

    _pre_cfu.io.pc_check_error := 0.U

    // _pc2if.io.PC_next           := Pc_Next
    // _pc2if.io.InstUnalignedP    := _mmu.io.i_unaligned
    // _pc2if.io.PhyAddrP          := _mmu.io.i_paddr
    // _pc2if.io.ExceptionW        := _cp0.io.exception
//-------------fec_1 其实就是在cache 中强行增加一个模块----------
    val stage_fec_1_stall = stage2_stall//_pre_cfu.io.stage_fec_1_stall
    val stage_fec_1_flush = _pre_cfu.io.stage_fec_1_flush
    val stage_fec_1_cached = RegInit(0.U(1.W))

    //stage_fec_1_stall  := stage2_stall


    val stage_fec_1_req =  RegInit(0.U.asBool)
    stage_fec_1_cached := Mux(stage_fec_1_flush,0.U,Mux(stage_fec_1_stall,inst_cache,stage_fec_1_cached))
    stage_fec_1_req := Mux(stage_fec_1_flush,0.U,Mux(stage_fec_1_stall,inst_req,stage_fec_1_req))
    val pc_valid = Mux(stage_fec_1_pc(4,2) <= 5.U,"b111".U,Mux(stage_fec_1_pc(4,2) === 6.U,"b011".U,"b001".U))
    stage_fec_1_pc_next := Mux(stage_fec_1_pc(4,2) <= 5.U || !stage_fec_1_cached.asBool, stage_fec_1_pc + 12.U,Cat(stage_fec_1_pc(31,5) + 1.U,0.U(5.W)))

    val req_wait = RegInit(0.U.asBool)

    
    // val bru = Module(new branch_prediction).io
    val stage_fec_1_pc_L = Module(new pc_detail)
    val stage_fec_1_pc_M = Module(new pc_detail)
    val stage_fec_1_pc_R = Module(new pc_detail)
    val stage_fec_1_pc_valid = RegInit(0.U(3.W))

    stage_fec_1_pc_valid := Mux(stage_fec_1_flush,0.U,Mux(stage_fec_1_stall,pc_valid,stage_fec_1_pc_valid))
  
    stage_fec_1_pc_L.io_in.pc_inst_in := 0.U
    stage_fec_1_pc_L.io_in.pc_value_in := pc_fetch
    stage_fec_1_pc_M.io_in.pc_inst_in := 0.U
    //===============================================可能要改
    stage_fec_1_pc_M.io_in.pc_value_in := pc_fetch + 4.U//Cat(pc_fetch(31,5),pc_fetch(4,2) + 1.U ,pc_fetch(1,0))
    stage_fec_1_pc_R.io_in.pc_inst_in := 0.U
    stage_fec_1_pc_R.io_in.pc_value_in := pc_fetch + 8.U//Cat(pc_fetch(31,5),pc_fetch(4,2) + 2.U ,pc_fetch(1,0))

    //===============================================可能要改
    //flush and stall
    stage_fec_1_pc_L.io.flush := stage_fec_1_flush
    stage_fec_1_pc_M.io.flush := stage_fec_1_flush
    stage_fec_1_pc_R.io.flush := stage_fec_1_flush

    stage_fec_1_pc_L.io.stall := stage_fec_1_stall
    stage_fec_1_pc_M.io.stall := stage_fec_1_stall
    stage_fec_1_pc_R.io.stall := stage_fec_1_stall


    stage_fec_1_pc := stage_fec_1_pc_L.io_out.pc_value_out
    
    // val bru = Module(new branch_prediction)
    // bru.pc := stage_fec_1_pc_L.io_out.pc_value_out
    // bru.pc_plus := stage_fec_1_pc_M.io_out.pc_value_out
    // bru.pc_plus_plus := stage_fec_1_pc_R.io_out.pc_value_out
    // val branch_state = Wire(Vec(3,Bool()))

    // branch_state(0) := Mux(stage_fec_1_pc_valid(0),bru.out_L,0.U)
    // branch_state(1) := Mux(stage_fec_1_pc_valid(1),bru.out_M,0.U)
    // branch_state(2) := Mux(stage_fec_1_pc_valid(2),bru.out_R,0.U)


    //-------------fec_2----------
    //this stage,we push 3 insts to the inst buffer, and make 2 insts out
    
    //_pre_cfu.io.inst_cache_working_on := inst_cache_working_on
    _pre_cfu.io.hit := inst_hit

    val stage_fec_2_stall = stage2_stall//_pre_cfu.io.stage_fec_2_stall
    val stage_fec_2_flush = stage2_flush


    val inst_buffer = Module(new fifo(16,64,3,2)).io
    val stage_fec_2_pc_L = Module(new pc_detail)
    val stage_fec_2_pc_M = Module(new pc_detail)
    val stage_fec_2_pc_R = Module(new pc_detail)
    val stage_fec_2_data_valid = RegInit(0.U.asBool) //这一阶段的指令
    val stage_fec_2_req = RegInit(0.U.asBool)

    stage_fec_2_req := Mux(stage_fec_2_flush,0.U,Mux(stage_fec_2_stall,stage_fec_1_req,stage_fec_2_req))
    stage_fec_2_data_valid := Mux(stage_fec_2_flush,0.U,Mux(stage_fec_2_stall,1.U,Mux((!inst_buffer.empty) && inst_buffer.point_write_en,0.U,stage_fec_2_data_valid)))

    stage_fec_2_pc_L.io_in.pc_inst_in := stage_fec_1_pc_L.io_out.pc_inst_out
    stage_fec_2_pc_L.io_in.pc_value_in := stage_fec_1_pc_L.io_out.pc_value_out

    stage_fec_2_pc_M.io_in.pc_inst_in := stage_fec_1_pc_M.io_out.pc_inst_out
    stage_fec_2_pc_M.io_in.pc_value_in := stage_fec_1_pc_M.io_out.pc_value_out 

    stage_fec_2_pc_R.io_in.pc_inst_in := stage_fec_1_pc_R.io_out.pc_inst_out
    stage_fec_2_pc_R.io_in.pc_value_in := stage_fec_1_pc_R.io_out.pc_value_out 

    //flush and stall
    stage_fec_2_pc_L.io.flush := stage_fec_2_flush
    stage_fec_2_pc_M.io.flush := stage_fec_2_flush
    stage_fec_2_pc_R.io.flush := stage_fec_2_flush

    stage_fec_2_pc_L.io.stall := stage_fec_2_stall
    stage_fec_2_pc_M.io.stall := stage_fec_2_stall
    stage_fec_2_pc_R.io.stall := stage_fec_2_stall

    inst_buffer.write_en    := Mux(stage_fec_2_data_valid,inst_write_en,0.U)
    inst_buffer.write_in(0) := Cat(inst_rdata_L,stage_fec_2_pc_L.io_out.pc_value_out)
    inst_buffer.write_in(1) := Cat(inst_rdata_M,stage_fec_2_pc_M.io_out.pc_value_out)
    inst_buffer.write_in(2) := Cat(inst_rdata_R,stage_fec_2_pc_R.io_out.pc_value_out)
    inst_buffer.read_en     := Mux(_cfu.io.StallF.asBool,1.U,0.U) //继续流水线流下去，就发射一条指令
    inst_buffer.point_flush := _cp0.io.exception


    stage2_flush := inst_buffer.point_write_en && !inst_buffer.empty


    _cfu.io.Inst_Fifo_Empty := inst_buffer.empty
    _pre_cfu.io.fifo_full := inst_buffer.full
    _pre_cfu.io.data_ok   := inst_data_ok
    // _pre_cfu.io.stage_fec_2_req := stage_fec_2_req
    _pre_cfu.io.stage2_stall := stage2_stall
    //同时也在写入，就不需要将上一个指令写入进行修改
    when(inst_buffer.point_write_en.asBool && inst_buffer.empty.asBool && inst_write_en === 0.U) {
        stage1_valid_flush := 2.U
    }.elsewhen(((inst_buffer.point_write_en.asBool && !inst_buffer.empty.asBool) || inst_buffer.point_flush.asBool) ){//此时fifo写入的任何数据都无用
        stage1_valid_flush := 1.U
    }.otherwise{
         stage1_valid_flush := 0.U
    }
    ready_to_branch := inst_buffer.point_write_en
    inst_buffer_full := inst_buffer.full


    
//full是用来控制前面流水线停止的，和这里无关
    val InDelaySlotF = RegInit(0.U.asBool)
    when((_cu.io.BranchD_Flag.asBool || _cu.io.JumpD.asBool) && !_cfu.io.StallF ) {
        InDelaySlotF := 1.U
    }.elsewhen(_cfu.io.StallF.asBool){
        InDelaySlotF := 0.U
    }.otherwise{
        InDelaySlotF := InDelaySlotF
    }
    //InDelaySlotF := Mux(_cfu.io.StallF.asBool,_cu.io.BranchD_Flag.asBool || _cu.io.JumpD.asBool,InDelaySlotF)
    val PCF =     _if2id.io.PCF 
    // _pc2if.io <> _imem.io
    _if2id.io.PCF            :=  inst_buffer.read_out(0)(31,0)
    _if2id.io.en             := _cfu.io.StallD //相当于清空ID阶段的所有数据
    _if2id.io.clr            := _cfu.io.FlushD
    _if2id.io.read_bank_pointF := inst_buffer.read_bank_point
    _if2id.io.read_length_pointF := inst_buffer.read_length_point
    // _if2id.io.PCPlus4F       := PCPlus4F
    // _if2id.io.PCPlus8F       := PCPlus8F
    _if2id.io.InstrF      := Mux(_cu.io1.BadInstrD.asBool || _cu.io1.SysCallD.asBool||_cu.io1.BreakD.asBool||
    _cu.io1.EretD.asBool,0.U,inst_buffer.read_out(0)(63,32))//如果延迟曹指令要跳转的话，就直接不运行这一条指令，避免对后面的操作产生影响
    // _if2id.io.ExceptionTypeF := ExceptionTypeF
    //_if2id.io.NextDelaySlotD := NextDelaySlotD   //下一条为延迟槽，ID阶段解码出来
    // _if2id.io.NextDelaySlotD :=   
    _if2id.io.NextDelaySlotD := (_cu.io.BranchD_Flag.asBool || _cu.io.JumpD.asBool) || InDelaySlotF //此时指令对齐才有可能为延迟曹
    
//-------------ID----------
    val BadVAddrD = Mux(Pc_Next(1,0) =/= 0.U,Pc_Next,0.U) 
    _regfile.io.A1 := InstrD(25,21)
    _regfile.io.A2 := InstrD(20,16) 
    _cu.io1.InstrD := _if2id.io.InstrD
    _br.io.rs := BranchRsD
    BranchRsD:= Mux(_cfu.io.ForwardAD.asBool,Forward_ResultM,_regfile.io.RD1)//前递，降低流水线阻塞,后面是指需要读寄存器的时候的值
    _br.io.rt := BranchRtD
    BranchRtD:= Mux(_cfu.io.ForwardBD.asBool,Forward_ResultM,_regfile.io.RD2)
    ExceptionTypeD_Out := Mux(_if2id.io.PCD(1,0) =/= 0.U,EXCEP_MASK_AdELI,0.U)
    _br.io.en := Mux(((ExceptionTypeD_Out | _id2ex.io.ExceptionTypeE_Out |//有例外的话
    _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out) === 0.U),1.U,0.U) //不允许触发分支
    _br.io.branch := _cu.io.BranchD
  
    
    _id2ex.io.ExceptionTypeD  := Mux( ExceptionTypeD_Out === 0.U,(
        (Mux(_cu.io1.BadInstrD.asBool,EXCEP_MASK_RI,0.U)) | 
        (Mux(_cu.io1.SysCallD.asBool,EXCEP_MASK_Sys,0.U)) |
        (Mux(_cu.io1.BreakD.asBool,EXCEP_MASK_Bp,0.U))    |
        (Mux(_cu.io1.EretD.asBool,EXCEP_MASK_ERET,0.U)) ),ExceptionTypeD_Out)

    _id2ex.io.RsD := RsD
    _id2ex.io.RtD := RtD 
    _id2ex.io.RdD := RdD 
    _id2ex.io.ImmD:= ImmD
    _id2ex.io.RD1D:= Mux(_cfu.io.ForwardAD.asBool,Forward_ResultM,_regfile.io.RD1)
    _id2ex.io.RD2D:= Mux(_cfu.io.ForwardBD.asBool,Forward_ResultM,_regfile.io.RD2)
    _id2ex.io.WriteCP0AddrD :=  InstrD(15,11)
    _id2ex.io.WriteCP0SelD  := InstrD(2,0)
    _id2ex.io.ReadCP0AddrD  := Mux(_cu.io1.EretD.asBool,"b01110".U , InstrD(15,11))
    _id2ex.io.ReadCP0SelD   := Mux(_cu.io1.EretD.asBool,0.U,InstrD(2,0))
    _id2ex.io.PCPlus8D      := PCPLus8D
    _id2ex.io.InDelaySlotD := _if2id.io.InDelaySlotD.asBool && (_if2id.io.PCD(1,0) === 0.U)
    _id2ex.io.PCD          := _if2id.io.PCD
    _id2ex.io.BranchJump_JrD  := Cat(0.U(1.W),_cu.io.JRD.asBool||_cu.io.BranchD_Flag.asBool || _cu.io.JumpD.asBool)
    _id2ex.io.BadVaddrD    := BadVAddrD

    inst_buffer.point_write_en := ((_cu.io.JumpD.asBool && _cfu.io.StallD.asBool) || (_br.io.exe.asBool && _cfu.io.StallD.asBool )) //&& !inst_buffer.empty
    _pre_cfu.io.branch_error := inst_buffer.point_write_en//(_cu.io.JumpD.asBool || _br.io.exe.asBool) //&& _cfu.io.StallD.asBool

//-------------EX----------
    //前递还没写
    val RD1ForWardE_p   = Mux2_4(_cfu.io.ForwardAE,_id2ex.io.RD1E,ResultW,Forward_ResultM,0.U)//rd1为rs对应的
    val RD2ForWardE_p   = Mux2_4(_cfu.io.ForwardBE,_id2ex.io.RD2E,ResultW,Forward_ResultM,0.U) //rd2为rt对应
    val RD1ForWardE_r   = RegInit(0.U(32.W))  
    val RD2ForWardE_r   = RegInit(0.U(32.W))
    val Forward_Lock1E  = RegInit(0.U(1.W)) 
    val Forward_Lock2E  = RegInit(0.U(1.W))
    val Forward_CP0_data = Mux(_cfu.io.ForwardCP0E.asBool,_ex2mem.io.WriteCP0HiLoDataM, _cp0.io.cp0_read_data)
    RD1ForWardE := Mux(Forward_Lock1E.asBool,RD1ForWardE_r,RD1ForWardE_p)
    RD2ForWardE := Mux(Forward_Lock2E.asBool,RD2ForWardE_r,RD2ForWardE_p)
    when(_cfu.io.StallE.asBool) {
        Forward_Lock1E := 0.U
        Forward_Lock2E := 0.U
    }.otherwise {
        when(_cfu.io.ForwardAE === "b01".U && !Forward_Lock1E.asBool) {//Rs,并且01表示ex阶段的寄存器与现在处于wb阶段的指令需要写入的指令相同
            Forward_Lock1E := 1.U//而写回阶段是知道了寄存器值应该是多少的，直接前递回来
            RD1ForWardE_r  := RD1ForWardE_p
        }
        when(_cfu.io.ForwardBE === "b01".U && !Forward_Lock2E.asBool){//Rs,并且01表示ex阶段的寄存器与现在处于wb阶段的指令需要写入的指令相同
            Forward_Lock2E := 1.U
            RD2ForWardE_r  := RD2ForWardE_p
        }
    }


    val Inst_badvaddrE = Mux(_id2ex.io.ExceptionTypeE_Out(31),Forward_CP0_data,_id2ex.io.BadVaddrE)//处于eret状态


    val WriteRegE = Mux2_4(_id2ex.io2.RegDstE,_id2ex.io.RtE,_id2ex.io.RdE,"b11111".U,"b00000".U)//选择到底用哪个寄存器来作为目的寄存器
    val WriteCP0HiLoDataE = Mux(_id2ex.io2.HiLoWriteE =/= 0.U, RD1ForWardE,Mux(
        _id2ex.io2.CP0WriteE.asBool,RD2ForWardE,0.U)) // 可能有问题
    val Src1E = Mux(_id2ex.io2.ALUSrcE(1).asBool, Cat(0.U(27.W), _id2ex.io.ImmE(10,6)),RD1ForWardE)//ImmE为ra段，为三种移位运算的操作数
    val Src2E = Mux(_id2ex.io2.ALUSrcE(0).asBool, _id2ex.io.ImmE, RD2ForWardE) //可能是常数，也可能是前递的数
    val CP0ToRegE = Mux(_id2ex.io.ExceptionTypeE_Out === 0.U,_id2ex.io.CP0ToRegE_Out, 0.U)
    _ex2mem.io.CP0ToRegE := CP0ToRegE//Mux(_id2ex.io.ExceptionTypeE_Out === 0.U,_id2ex.io.CP0ToRegE_Out, 0.U)
    _alu.io.in1 := Src1E
    _alu.io.in2 := Src2E
    _alu.io.ctrl := _id2ex.io2.ALUCtrlE
    
    _muldiv.io.en := Mux(_id2ex.io.ExceptionTypeE_Out =/= 0.U , 0.U,1.U)
    _muldiv.io.in1 := Src1E
    _muldiv.io.in2 := Src2E
    _muldiv.io.ctrl := Cat(_id2ex.io2.ALUCtrlE(9,8),_id2ex.io2.ALUCtrlE(6,5))

    val dmemreq_start =( _id2ex.io2.MemRLE =/= 0.U) && (_id2ex.io2.MemWriteE.asBool)//需要写寄存器并且那个煞笔rl不等于零，便说明出现了问题

    val HiInE = Mux(_id2ex.io2.HiLoWriteE === "b10".U,WriteCP0HiLoDataE,_muldiv.io.hi) // b10 对应MTHI指令，通用寄存器到HI寄存器
    val LoInE = Mux(_id2ex.io2.HiLoWriteE === "b01".U,WriteCP0HiLoDataE,_muldiv.io.lo) // b10 对应MTHI指令，通用寄存器到HI寄存器

    BadVAddrE:= Mux(_mmu.io.d_unaligned.asBool,_mmu.io.d_vaddr,Inst_badvaddrE) //mmu模块发现数据指令错位,下面还需要加上数据错误例外 直接加法计算
    
    _dmemreq.io.en := Mux((_id2ex.io.ExceptionTypeE_Out | _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out
       ) =/= 0.U /*||Exception_state.asBool*/,0.U,((!_mmu.io.d_unaligned.asBool && _cfu.io.StallE.asBool) || mem_write)) // 后面几级流水线没有出现例外，加上流水线没有停止，
        // 再加上有mem到reg的操作或者写Mem的操作，才会有所谓的mem access，其实就是使能memreq
    _dmemreq.io.MemToRegE := _id2ex.io2.MemToRegE
    _dmemreq.io.MemWidthE := _id2ex.io2.MemWidthE
    _dmemreq.io.PhyAddrE  := _mmu.io.d_paddr
    _dmemreq.io.MemWriteE := _id2ex.io2.MemWriteE
    // _dmemreq.io.addr_ok   := io.data_addr_ok
    _dmemreq.io.addr_ok   := data_addr_ok
    _dmemreq.io.WriteDataE := WriteDataE


    //读寄存器HI 和 LO
    _ex2mem.io.HiLoOutE := Mux1H(Seq(
        _id2ex.io2.HiLoToRegE(0) -> Mux2_4(_cfu.io.ForwardHE,_hilo.io.lo_o,LoInW,LoInM ,0.U),
        _id2ex.io2.HiLoToRegE(1) -> Mux2_4(_cfu.io.ForwardHE,_hilo.io.hi_o,HiInW,HiInM ,0.U)
    ))
   
    val temp_exceptionE = Mux(_id2ex.io.ExceptionTypeE_Out =/= 0.U,_id2ex.io.ExceptionTypeE_Out,
        (Mux(_mmu.io.d_unaligned.asBool && _id2ex.io2.MemToRegE.asBool ,EXCEP_MASK_AdELD,0.U)) |
        (Mux(_mmu.io.d_unaligned.asBool && _id2ex.io2.MemWriteE.asBool ,EXCEP_MASK_AdES,0.U))  |
        (Mux(_alu.io.overflow.asBool,EXCEP_MASK_Ov,0.U))) //取或运算便于搞事,之后要多减少位数，感觉32位稍微有点长了

     _ex2mem.io.ExceptionTypeE  :=  Mux(_id2ex.io.ExceptionTypeE_Out(31) && Forward_CP0_data(1,0) =/= 0.U,EXCEP_MASK_AdELI,0.U) | temp_exceptionE
    
    _ex2mem.io.ReadCP0DataE :=  Forward_CP0_data//cp0的读取数据，加上了前di
    _ex2mem.io.CP0WriteE    := _id2ex.io2.CP0WriteE
    _ex2mem.io.ALUOutE := _alu.io.result
    _ex2mem.io.BadVAddrE := BadVAddrE
    _ex2mem.io.HiInE    := HiInE
    _ex2mem.io.LoInE    := LoInE
    _ex2mem.io.WriteRegE := WriteRegE//要写的寄存器
    _ex2mem.io.WriteDataE := WriteDataE
    _ex2mem.io.WriteCP0HiLoDataE := WriteCP0HiLoDataE
    _ex2mem.io.PhyAddrE   := _mmu.io.d_paddr
    _ex2mem.io.RtE        := RD2ForWardE



    _cp0.io.cp0_read_addr := _id2ex.io2.ReadCP0AddrE
    _cp0.io.cp0_read_sel := _id2ex.io2.ReadCP0SelE
    _cp0.io.cp0_write_addr := _mem2wb.io.WriteCP0AddrW
    _cp0.io.cp0_write_sel :=_mem2wb.io.WriteCP0SelW
    _cp0.io.cp0_write_data := _mem2wb.io.WriteCP0HiLoDataW

    _hilo.io.hi_i := _mem2wb.io.HiInW
    _hilo.io.lo_i := _mem2wb.io.LoInW//写回阶段，将寄存器的值写回
    


// 钢架的
// 钢架的

// ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！

//-------------MEM----------
    val Mem_withRL_Data    = Wire(UInt(32.W)) 

    Forward_ResultM := MuxCase(_ex2mem.io.ALUOutM,Seq(
       // _ex2mem.io.MemToRegM.asBool -> Mem_withRL_Data ,//刚加的
        _ex2mem.io.LinkM.asBool -> PCPlus8M,
        _ex2mem.io.CP0ToRegM.asBool -> _ex2mem.io.ReadCP0DataM,
       ( _ex2mem.io.HiLoToRegM =/= 0.U) -> _ex2mem.io.HiLoOutM
    ))
    ResultM := MuxCase(_ex2mem.io.ALUOutM,Seq(
       _ex2mem.io.MemToRegM.asBool -> Mem_withRL_Data ,//刚加的
        _ex2mem.io.LinkM.asBool -> PCPlus8M,
        _ex2mem.io.CP0ToRegM.asBool -> _ex2mem.io.ReadCP0DataM,
       ( _ex2mem.io.HiLoToRegM =/= 0.U) -> _ex2mem.io.HiLoOutM
    ))



        // --------------
    Mem_withRL_Data := _dmem.io.RD /*Mux(_ex2mem.io.MemRLM === 0.U,_dmem.io.RD,Mux(_ex2mem.io.MemRLM === "b10".U,
        MuxLookup(_ex2mem.io.PhyAddrM(1,0),0.U,Seq(
            0.U -> Cat(ReadData_dmem(7,0),RtM(23,0)),
            1.U -> Cat(ReadData_dmem(15,0),RtM(15,0)),
            2.U -> Cat(ReadData_dmem(23,0),RtM(7,0)),
            3.U -> ReadData_dmem
        )),MuxLookup(_ex2mem.io.PhyAddrM(1,0),0.U,Seq(
            0.U -> ReadData_dmem,
            1.U -> Cat(RtM(31,24),ReadData_dmem(31,8)),
            2.U -> Cat(RtM(31,16),ReadData_dmem(31,16)),
            3.U -> Cat(RtM(31,8),ReadData_dmem(31,24))))))*/
  
    _mem2wb.io.ReadDataM := Mem_withRL_Data 
    _mem2wb.io.ExceptionTypeM := _ex2mem.io.ExceptionTypeM_Out

    _mem2wb.io.ResultM        := ResultM
    _mem2wb.io.BranchJump_JrM  := _ex2mem.io.BranchJump_JrM

    _mem2wb.io.BadVAddrM :=  Mux(_ex2mem.io.ExceptionTypeM_Out(EXCEP_AdELI) && (_ex2mem.io.BadVAddrM(31) === 0.U 
        || _ex2mem.io.BadVAddrM(31,30) === "b11".U ),_ex2mem.io.PCM,_ex2mem.io.BadVAddrM)

    // _mem2wb.io.MemToRegM_Forward_hasStall :=&& _ex2mem.io.RegWriteM.asBool &&  _ex2mem.io.MemToRegM.asBool && ((RsD =/= 0.U && RsD === _ex2mem.io.WriteRegM ) || 
    //             (RtD =/= 0.U && RtD ===  _ex2mem.io.WriteRegM &&  _ex2mem.io.RegWriteM.asBool) //mem阶段出现mem2reg并且此时需要前递时，停止流水线
    //             || (_id2ex.io.RsE =/= 0.U && _id2ex.io.RsE === _ex2mem.io.WriteRegM && _id2ex.io2.RegWriteE && _id2ex.io2.Write

    //                 io.ForwardAE := Mux(io.RsE =/= 0.U && io.RsE === io.WriteRegM && io.RegWriteM.asBool,"b10".U,Mux(
    //     io.RsE =/= 0.U && io.RsE === io.WriteRegW && io.RegWriteW.asBool,"b01".U,0.U))

    // io.ForwardBE := Mux(io.RtE =/= 0.U && io.RtE === io.WriteRegM && io.RegWriteM.asBool,"b10".U,Mux(
    //     io.RtE =/= 0.U && io.RtE === io.WriteRegW && io.RegWriteW.asBool,"b01".U,0.U))
    // //防止前面延迟太高，在这里处理地址



//-------------WB----------commit ----  
    //ResultW   := Mux(_mem2wb.io.MemToRegW.asBool,_mem2wb.io.ReadDataW,_mem2wb.io.ResultW)
    ResultW   := _mem2wb.io.ResultW//Mux(_mem2wb.io.MemToRegW_Forward_hasStall.asBool,_mem2wb.io.ReadDataW,_mem2wb.io.ResultW)
    RegWriteW := Mux(_mem2wb.io.ExceptionTypeW_Out =/= 0.U,0.U,_mem2wb.io.RegWriteW_Out)
    val HiLoWriteW = Mux(_mem2wb.io.ExceptionTypeW_Out =/= 0.U,0.U,_mem2wb.io.HiLoWriteW) //发生例外，全都清空
    val CP0WriteW  = Mux(_mem2wb.io.ExceptionTypeW_Out =/= 0.U,0.U,_mem2wb.io.CP0WriteW)
    val ExceptionTypeW  = _mem2wb.io.ExceptionTypeW_Out
    // ResultW   := Mux(_mem2wb.io.MemToRegW.asBool,_)
    _regfile.io.WD3 := ResultW
    _regfile.io.A3  := _mem2wb.io.WriteRegW
    _regfile.io.WE3 := RegWriteW

    _hilo.io.we   := HiLoWriteW //没有例外就写寄存器
    


    // -----------------others that I can not understand
    val disable_cache = RegInit(0.U(1.W))
    disable_cache := Mux((PCF === 0xbfc0005.U<<4 ),1.U,disable_cache)

    _mmu.io.i_en := 1.U
    //我寻思就一个mmu也没啥好ban的
    _mmu.io.d_en := Mux((_id2ex.io.ExceptionTypeE_Out | _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out)
        =/= 0.U,0.U,_cfu.io.StallE & (_id2ex.io2.MemToRegE | _id2ex.io2.MemWriteE))
    _mmu.io.d_clr := 0.U  
    _mmu.io.d_width := _id2ex.io2.MemWidthE  
    // _mmu.io.i_vaddr := _pc2if.io.PCP
    _mmu.io.d_vaddr :=  Src1E + Src2E//_alu.io.result//alu里面的结果计算出来可能就要用来取数据 // 数据地质一定是加法算出来的，不经过alu里面一堆高延迟路径
    _mmu.io.d_memrl := _id2ex.io2.MemRLE | _ex2mem.io.MemRLM
    
              

    data_cache :=  Mux(mem_write,mem_cached, _mmu.io.d_cached ) //
    data_size := Mux(mem_write,2.U,_dmemreq.io.size)

   // inst_cache := !disable_cache & _mmu.io.i_cached
  

    // _cp0.io.int_i := io.ext_int
    _cp0.io.int_i := 0.U
    _cp0.io.pc    := Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.PCW,PCW_Reg)//_mem2wb.io.PCW,方便调试罢了
    _cp0.io.mem_bad_vaddr := _mem2wb.io.BadVAddrW
    _cp0.io.cp0_write_en  := CP0WriteW 
    _cp0.io.exception_type_i := ExceptionTypeW
    _cp0.io.in_delayslot     :=Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.InDelaySlotW,slot_Reg)
    _cp0.io.in_branchjump_jr   :=Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.BranchJump_JrW,branchjump_Jr_Reg)
///----------------------------------------------------------------                                         
    _cfu.io.InException := _cp0.io.exception























//===               ==  =       ==  =   =   =   =   =   =   =   ==  ==  
    // _cfu.io.InstUnalignedF := _pc2if.io.InstUnalignedF
    _cfu.io.BranchD      := _cu.io.BranchD
    _cfu.io.JumpD        := _cu.io.JumpD
    _cfu.io.JRD         := _cu.io.JRD
    _cfu.io.CanBranchD  := Mux((ExceptionTypeD_Out | _id2ex.io.ExceptionTypeE_Out | 
        _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out) =/= 0.U, 0.U,1.U)
    _cfu.io.DivPendingE         := _muldiv.io.pending
    _cfu.io.AddrPendingE        := _dmemreq.io.addr_pending //data地址等待
    _cfu.io.DataPendingM        := _dmem.io.data_pending // data数据等待
    _cfu.io.AddrPendingF        := 0.U // 指令地址等待
    _cfu.io.DataPendingF        := inst_buffer.empty //指令数据等待

    _cfu.io.WriteRegE           := WriteRegE
    _cfu.io.MemToRegE           := _id2ex.io2.MemToRegE
    _cfu.io.RegWriteE           := _id2ex.io2.RegWriteE
    _cfu.io.HiLoToRegE          := _id2ex.io2.HiLoToRegE
    _cfu.io.CP0ToRegE           := CP0ToRegE

    _cfu.io.WriteRegM           := _ex2mem.io.WriteRegM
    _cfu.io.MemToRegM           := _ex2mem.io.MemToRegM
    _cfu.io.RegWriteM           := _ex2mem.io.RegWriteM
    _cfu.io.CP0WriteM           := _ex2mem.io.CP0WriteM
    _cfu.io.HiLoWriteM          := _ex2mem.io.HiLoWriteM

    _cfu.io.ReadCP0AddrE         := _id2ex.io2.ReadCP0AddrE
    _cfu.io.ReadCP0SelE        := _id2ex.io2.ReadCP0SelE

    _cfu.io.WriteCP0AddrM        := _ex2mem.io.WriteCP0AddrM 
    _cfu.io.WriteCP0SelM        := _ex2mem.io.WriteCP0SelM

    _cfu.io.CP0WriteW           := _mem2wb.io.CP0WriteW
    _cfu.io.HiLoWriteW          := _mem2wb.io.HiLoWriteW
    _cfu.io.WriteRegW           := _mem2wb.io.WriteRegW
    _cfu.io.RegWriteW           := RegWriteW

    _cfu.io.RsD                 := RsD
    _cfu.io.RtD                 := RtD
    _cfu.io.RsE                 := _id2ex.io.RsE
    _cfu.io.RtE                 := _id2ex.io.RtE   
    _cfu.io.MemRLE              := _id2ex.io2.MemRLE

    }
}

object myCPU_core_test extends App{
    (new ChiselStage).emitVerilog(new myCPU)
}
