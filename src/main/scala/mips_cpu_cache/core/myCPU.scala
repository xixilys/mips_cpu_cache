package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import os.makeDir
//import mips_cpu_2nd.inst_cache_2nd
/*
目前已经禁用了swl和lwl相关的功能功能
若想启用，修改  Mem_withRL_Data := _dmem.io.RD
修改mem_write



*/

//制作成虚地址cache
//当出现进程切换时，自动清空所有的cache，其实就是直接把tag全部unvalid掉

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
    val   inst_rdata_L = IO(Input(UInt(40.W))).suggestName("inst_sram_rdata_L") 
    // val   inst_rdata_M = IO(Input(UInt(40.W))).suggestName("inst_sram_rdata_M") 
    // val   inst_rdata_R = IO(Input(UInt(40.W))).suggestName("inst_sram_rdata_R") 

    //val   inst_cache_working_on = IO(Input(Bool())).suggestName("cache_working_on")   
    //val   inst_rdata = IO(Input(UInt(32.W))).suggestName("inst_sram_rdata") 
    val   inst_hit = IO(Input(UInt(1.W))).suggestName("inst_sram_hit")
    val   inst_valid = IO(Input(UInt(3.W))).suggestName("inst_data_valid")//代表回来的数据哪一位有效
    val   inst_write_en = IO(Input(UInt(2.W))).suggestName("inst_write_en")//代表回来的数据哪一位有效

    val   inst_ready_branch = IO(Output(Bool()))
    val   inst_buffer_empty  =  IO(Output(Bool()))
    val   cp0_asid      = IO(Output(UInt(8.W)))
    val   inst_tlb_exception = IO(Input(UInt(2.W)))


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

    val   data_stage2_stall = IO(Input(Bool()))
    val   data_tlb_exception = IO(Input(UInt(3.W)))
    val   data_wstrb = IO(Output(UInt(4.W)))

    val   tlbp_search_vaddr = IO(Output(UInt(32.W)))
    val   tlbp_search_en   = IO(Output(Bool()))
    val   tlb_search_index = IO(Input(UInt(tlb_index_width.W)))
    val   tlb_search_hit   = IO(Input(Bool()))

    val   tlb_read_index   = IO(Output(UInt(tlb_index_width.W)))
    val   tlb_write_index  = IO(Output(UInt(tlb_index_width.W)))

   // val   data_hit = IO(Input(UInt(1.W)))
   
    val   debug_wb_pc       = IO(Output(UInt(32.W)))
    val   debug_wb_rf_wen   = IO(Output(UInt(4.W)))
    val   debug_wb_rf_wnum  = IO(Output(UInt(5.W)))
    val   debug_wb_rf_wdata = IO(Output(UInt(32.W)))

    val   cp0_tlb_read_data  = IO(Flipped(new tlb_write_or_read_port))
    val   cp0_tlb_write_data = IO(new tlb_write_or_read_port)

    val   tlb_write_en      = IO(Output(Bool()))

        

    override def desiredName = "myCPU"
withClockAndReset(clk.asClock,(~resetn).asAsyncReset) {
    def jr_Decoder(value : UInt):UInt = {
        val opD = value(31,26)
        val RtD = value(20,16)
        val FunctD = value(5,0)
        MuxLookup(opD,0.U,Seq(  
        OP_SPECIAL -> MuxLookup(FunctD,0.U,Seq( 
            FUNC_JR   ->   (1).U,
            FUNC_JALR ->   (1.U)))))
    }
    


// ------实例化所有模块,并且连接部分线
    val _alu = Module(new alu)
    val _br  = Module(new br)
    val _cfu = Module(new cfu)
    val _cp0 = Module(new cp0)
    val _cu  = Module(new cu)
    val _dmem = Module(new dmem)
    val _dmemreq = Module(new dmemreq)
    val _ex2mem  = Module(new ex2mem)
    val _mem2mem2  = Module(new ex2mem)
    val _hilo    = Module(new hilo)
    val _id2ex   = Module(new id2ex)
    val _if2id   = Module(new if2id(5,2))
    // val _imem    = Module(new imem)
    val _mem22wb  = Module(new mem2wb)
    val _addr_cal     = Module(new addr_cal)
    val _muldiv  = Module(new muldiv)
    // val _pc2if   = Module(new pc2if)
    val _regfile = Module(new regfile)

    val inst_buffer = Module(new fifo(16,(64 + 7 + 2 + 32 + 4 + 7 + 1 + 8 + 8 + 1 + 2),3,2)).io

    // io.inst_port_test <> _pc2if.io.inst_port_test
    // inst_port_test <> _pc2if.io.inst_port_test
//    _if2id.io <> _pc2if.io
   _cu.io     <> _id2ex.io1
   _id2ex.io2 <> _ex2mem.io1

// -----------定义所有相关的变量-------
//-------------PC----------
    inst_wr   := 0.U//_pc2if.io.inst_port_test.inst_wr
//-------------fec_1 其实就是在cache 中强行增加一个模块----------

    val stage_fec_2_branch_answer = Wire(Bool())
    val stage_fec_2_branch_target = Wire(UInt(32.W))
    

val stage_fec_1_pc = Wire(UInt(32.W))

//-------------IF----------
    val stage_fec_2_inst_jump = inst_rdata_L(33)
    val stage_fec_2_inst_branch = inst_rdata_L(32)

    val pre_decoder_branchD_flag  = RegInit(0.U.asBool)
    val pre_decoder_branchdata   = RegInit(0.U(6.W)) 
    val pre_decoder_jump    =   RegInit(0.U.asBool)
    val pre_decoder_jr      =   RegInit(0.U.asBool)

    //val ExceptionTypeF = Mux(_pc2if.io.InstUnalignedF.asBool,EXCEP_MASK_AdELI,0.U)//3）取指PC不对齐于字边界，指令不对齐例外,由mmu处获得
   
    //-------------ID----------
    // val branch_error = Wire(Bool())
    val ExceptionTypeD_Out = Wire(UInt(32.W))
    _id2ex.io.en            :=  _cfu.io.StallD
    _id2ex.io.clr           :=  _cfu.io.FlushE
    val InstrD = _if2id.io.InstrD
    val BranchRsD = Wire(UInt(32.W))//Mux(_cfu.io.ForwardAD.asBool,ResultM,_regfile.io.RD1)//前递，降低流水线阻塞,后面是指需要读寄存器的时候的值
    val BranchRtD = Wire(UInt(32.W))
    val PCPLus4D = _if2id.io.PCPlus4D
    val PCPLus8D = _if2id.io.PCPlus8D
    val PCSrcD  = Cat((pre_decoder_jump.asBool && _cfu.io.StallD.asBool),_br.io.exe.asBool && _cfu.io.StallD.asBool)//Wire(UInt(2.W)) 要不要跳转 + 有没有分支指令需要跳转
    val PCBranchD = Cat(Cat(Seq.fill(14)(_if2id.io.InstrD(15))),_if2id.io.InstrD(15,0),0.U(2.W)) + PCPLus4D//对指令进行有符号扩展之后的东西
    val PCJumpD   = Mux(pre_decoder_jr.asBool,BranchRsD,Cat(PCPLus4D(31,28),InstrD(25,0),0.U(2.W)) )// 分支指令均需要读寄存器并且进行比较,由于我是在解码阶段即判断到底需不需要跳转，所以需要用id 阶段的pc进行运算
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

    data_addr := _dmemreq.io.addr
    data_wdata := Mux(mem_write,mem_write_data_rl,_dmemreq.io.wdata)
    data_wstrb := _dmemreq.io.wstrb

    data_wr := _dmemreq.io.wr//无论是写的还是读的LR，在exe阶段都得取数据
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
    // val ResultM = Wire(UInt(32.W))
    val HiInM   = _ex2mem.io.HiInM
    val LoInM   = _ex2mem.io.LoInM
    val PCPlus8M = _ex2mem.io.PCPlus8M
    val Forward_ResultM = Wire(UInt(32.W))


//-------------MEM2----------
val Forward_ResultM2 = Wire(UInt(32.W))
    _dmem.io.req        := _dmemreq.io.req
    _dmem.io.addr_ok    := data_addr_ok
    _dmem.io.data_ok    := data_stage2_stall
    _dmem.io.rdata      := data_rdata
    _dmem.io.ReadEn     := _mem2mem2.io.MemToRegM
    _dmem.io.WIDTH      := _mem2mem2.io.MemWidthM
    _dmem.io.SIGN := !_mem2mem2.io.LoadUnsignedM.asBool
    // _dmem.io.WE   :=  _ex2mem.io.MemWriteM
    // _dmem.io.WIDTH := _ex2mem.io.MemWidthM
    // _dmem.io.Physisc_Address := _ex2mem.io.PhyAddrM
    // _dmem.io.SIGN       := _ex2mem.io.LoadUnsignedM
    _dmem.io.Physisc_Address := _mem2mem2.io.PhyAddrM



    _mem22wb.io.en            :=  _cfu.io.StallW
    _mem22wb.io.clr           := _cfu.io.FlushW

    _mem22wb.io.RegWriteM            := _mem2mem2.io.RegWriteM
    _mem22wb.io.MemToRegM            := _mem2mem2.io.MemToRegM
    _mem22wb.io.WriteRegM            := _mem2mem2.io.WriteRegM
    _mem22wb.io.HiInM                := _mem2mem2.io.HiInM
    _mem22wb.io.LoInM                := _mem2mem2.io.LoInM
    _mem22wb.io.InDelaySlotM         := _mem2mem2.io.InDelaySlotM

    _mem22wb.io.PCM                  := _mem2mem2.io.PCM
    _mem22wb.io.CP0WriteM            := _mem2mem2.io.CP0WriteM
    _mem22wb.io.WriteCP0AddrM        := _mem2mem2.io.WriteCP0AddrM 
    _mem22wb.io.WriteCP0SelM         := _mem2mem2.io.WriteCP0SelM
    _mem22wb.io.WriteCP0HiLoDataM    := _mem2mem2.io.WriteCP0HiLoDataM
    _mem22wb.io.HiLoWriteM           := _mem2mem2.io.HiLoWriteM 
    _mem22wb.io.Tlb_ControlM         := _mem2mem2.io.Tlb_ControlM

//-------------WB----------

    val HiInW     =  _mem22wb.io.HiInW
    val LoInW     =  _mem22wb.io.LoInW//
    val ResultW   =  Wire(UInt(32.W))
    val RegWriteW =  Wire(UInt(1.W))

    val PCW_Reg = RegInit(0.U(32.W))
    val slot_Reg = RegInit(0.U(1.W))
    val branchjump_Jr_Reg = RegInit(0.U(2.W))
    val Exception_state_Reg = RegInit(0.U(1.W))
    PCW_Reg := Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.PCW,PCW_Reg)
    slot_Reg := Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.InDelaySlotW,slot_Reg)
    branchjump_Jr_Reg := Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.BranchJump_JrW,branchjump_Jr_Reg)
    Exception_state_Reg := Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.ExceptionTypeW_Out =/= 0.U,Exception_state_Reg )
    val Exception_state =  Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.ExceptionTypeW_Out =/= 0.U,Exception_state_Reg  )
    // val wb_expction_type
    val reg_pc = RegInit(0.U(32.W))
    reg_pc := _mem22wb.io.PCW
    debug_wb_pc := _mem22wb.io.PCW
    debug_wb_rf_wen := Mux(reg_pc === _mem22wb.io.PCW,0.U,Mux(RegWriteW.asBool,0xf.U,0.U))
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
    //pc_value := Mux(reset.asBool,Cat(0xbfbf.U,0xfff4.U),Mux(io.flush,0.U,Mux(io.stall,io_in.pc_value_in,pc_value)))
    pc_value := Mux(reset.asBool,Cat(0xbfbf.U,0xfffc.U),Mux(io.flush,0.U,Mux(io.stall,io_in.pc_value_in,pc_value)))
    io_out.pc_value_out := pc_value
    val pc_inst = RegInit(0.U(32.W))
    pc_inst  := Mux(io.flush,0.U,Mux(io.stall,io_in.pc_inst_in,pc_inst))
    io_out.pc_inst_out  := pc_inst
}
//cfu
    val _pre_cfu = Module(new pre_cfu)
 // ---------- PC_cal ----------
    //_pc2if.io <> io//sram like增加AXI总线接口，仅仅增加了一个握手
       val ready_to_branch_pre = Wire(Bool())
    //pc_next 不在這裡算
    val stage_fec_1_pc_next = Wire(UInt(32.W))
    val Pc_Next = Wire(UInt(32.W))
    val Pc_Next_normal = Wire(UInt(32.W))
    val pc_next_wait = RegInit(0.U(32.W))

    val ready_to_branch = Wire(Bool())
    pc_next_wait := Mux(ready_to_branch || ready_to_branch_pre,Pc_Next_normal,pc_next_wait)
    val pc_req_wait = RegInit(0.U.asBool)
    // val branch_pre_wait = RegInit(0.U.asBool)


    when(!inst_req.asBool && (ready_to_branch || ready_to_branch_pre) && !_cp0.io.exception.asBool ) {
        pc_req_wait := 1.U
    }.elsewhen((pc_req_wait && inst_req.asBool ) || _cp0.io.exception.asBool || (ready_to_branch && inst_req.asBool)) {
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
    
// Mux(_cp0.io.exception.asBool,_cp0.io.return_pc,Mux(returnPc_req_wait,exception_Pc_reg,Mux(pc_req_wait && !(ready_to_branch /*|| ready_to_branch_pre*/),pc_next_wait,Pc_Next_normal)))
//发现分支有错误需要修改的时候，这时候如果pc_req_wait变化的话，就需要判断
    Pc_Next := Mux(_cp0.io.exception.asBool,_cp0.io.return_pc,Mux(returnPc_req_wait,exception_Pc_reg,Mux(ready_to_branch,Pc_Next_normal,Mux(pc_req_wait,pc_next_wait,Pc_Next_normal))))



    val pc_fetch = Wire(UInt(32.W)) // 这玩意是最后真的扔到cache中的指针
    pc_fetch := Pc_Next
    inst_cache  := check_cached(Pc_Next)
    inst_req    := stage2_stall//_pre_cfu.io.stage_pc_cal_stall
    inst_ready_to_use := Pc_Next(1,0) === 0.U 
    //虚地址cache
    inst_addr   := Pc_Next

    inst_size := 2.U // 一直都是32位，也就是2.U
    inst_wdata := 0.U //写请求的写数据，咱们只需要读数据即可

    _pre_cfu.io.pc_check_error := 0.U


//-------------fec_1 其实就是在cache 中强行增加一个模块----------

val  commit_cache_reg = RegInit(1.U.asBool)
val  commit_bru_reg = RegInit(1.U.asBool)
commit_cache_reg := Mux(_cfu.io.StallE.asBool && _cu.io1.commit_cache_ins,!commit_cache_reg,commit_cache_reg)
commit_bru_reg := Mux(_cfu.io.StallE.asBool && commit_bru_reg,!_cu.io1.commit_cache_ins,commit_bru_reg)
    val stage_fec_1_stall = stage2_stall//_pre_cfu.io.stage_fec_1_stall
    val stage_fec_1_flush = _cp0.io.exception.asBool && !stage2_stall
    val stage_fec_1_cached = RegInit(0.U(1.W))


    //stage_fec_1_stall  := stage2_stall


    val stage_fec_1_req =  RegInit(0.U.asBool)
    stage_fec_1_cached := Mux(stage_fec_1_flush,0.U,Mux(stage_fec_1_stall,inst_cache,stage_fec_1_cached))
    stage_fec_1_req := Mux(stage_fec_1_flush,0.U,Mux(stage_fec_1_stall,inst_req,stage_fec_1_req))
    //val pc_valid = Mux(stage_fec_1_pc(4,2) <= 5.U,"b111".U,Mux(stage_fec_1_pc(4,2) === 6.U,"b011".U,"b001".U))
    val pc_valid = 1.U
    //stage_fec_1_pc_next := Mux(stage_fec_1_pc(4,2) <= 5.U || !stage_fec_1_cached.asBool, stage_fec_1_pc + 12.U,Cat(stage_fec_1_pc(31,5) + 1.U,0.U(5.W)))

    stage_fec_1_pc_next := Mux(ready_to_branch_pre,stage_fec_2_branch_target,stage_fec_1_pc + 4.U)
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
    stage_fec_1_pc_M.io_in.pc_value_in := pc_fetch//Cat(pc_fetch(31,5),pc_fetch(4,2) + 1.U ,pc_fetch(1,0))
    // pc_fetch + 4.U//
    stage_fec_1_pc_R.io_in.pc_inst_in := 0.U
    stage_fec_1_pc_R.io_in.pc_value_in := pc_fetch//Cat(pc_fetch(31,5),pc_fetch(4,2) + 2.U ,pc_fetch(1,0))
    // pc_fetch + 8.U//

    //===============================================可能要改
    //flush and stall
    stage_fec_1_pc_L.io.flush := stage_fec_1_flush
    stage_fec_1_pc_M.io.flush := stage_fec_1_flush
    stage_fec_1_pc_R.io.flush := stage_fec_1_flush

    stage_fec_1_pc_L.io.stall := stage_fec_1_stall
    stage_fec_1_pc_M.io.stall := stage_fec_1_stall
    stage_fec_1_pc_R.io.stall := stage_fec_1_stall


    stage_fec_1_pc := stage_fec_1_pc_L.io_out.pc_value_out
    

    val bru = Module(new branch_prediction_with_blockram).io
    bru.pc := stage_fec_1_pc_L.io_out.pc_value_out
    bru.pc_plus := bru.pc//stage_fec_1_pc_M.io_out.pc_value_out
    bru.pc_plus_plus := bru.pc//stage_fec_1_pc_R.io_out.pc_value_out
    bru.sram_pc := Pc_Next

    val stage_fec_1_valid = RegInit(0.U.asBool)

    val access_stage1_sram_valid =  !((inst_buffer.point_write_en.asBool && (!inst_buffer.empty.asBool || (inst_buffer.empty.asBool && inst_write_en =/= 0.U))) || inst_buffer.point_flush.asBool) && !_cp0.io.exception.asBool

    stage_fec_1_valid := Mux(stage_fec_1_stall,1.U,Mux(inst_buffer.point_write_en.asBool,access_stage1_sram_valid,stage_fec_1_valid))

    inst_buffer_empty := inst_buffer.empty
    inst_ready_branch := ready_to_branch
    //-------------fec_2----------
    //this stage,we push 3 insts to the inst buffer, and make 2 insts out
    

    _pre_cfu.io.hit := inst_hit

    val stage_fec_2_stall = stage2_stall//_pre_cfu.io.stage_fec_2_stall
    val stage_fec_2_flush = stage2_flush

    bru.stage2_flush := stage_fec_2_flush
    bru.stage2_stall := stage_fec_2_stall


    val stage_fec_2_valid = RegInit(0.U.asBool)
    stage_fec_2_valid := Mux(stage_fec_2_stall,Mux(inst_buffer.point_write_en,access_stage1_sram_valid,stage_fec_1_valid),stage_fec_2_valid)

    val stage_fec_2_bht  = RegInit(VecInit(Seq.fill(3)(0.U(7.W))))
    val stage_fec_2_pht  = Wire(Vec(3,UInt(2.W)))
    val stage_fec_2_pre_target = Wire(Vec(3,UInt(32.W)))

    val stage_fec_2_hascode  = RegInit(VecInit(Seq.fill(3)(0.U(4.W))))
    val stage_fec_2_lookup_data  = Wire(Vec(3,UInt(7.W)))

    stage_fec_2_lookup_data := bru.lookup_data


    stage_fec_2_bht(0) := Mux(stage2_flush,0.U,Mux(stage2_stall,bru.bht_L,stage_fec_2_bht(0)))
    stage_fec_2_bht(1) := Mux(stage2_flush,0.U,Mux(stage2_stall,bru.bht_M,stage_fec_2_bht(1)))
    stage_fec_2_bht(2) := Mux(stage2_flush,0.U,Mux(stage2_stall,bru.bht_R,stage_fec_2_bht(2)))

    stage_fec_2_pht(0) := bru.out_L
    stage_fec_2_pht(1) := bru.out_M
    stage_fec_2_pht(2) := bru.out_R

    stage_fec_2_pre_target(0) := bru.pre_target_L
    stage_fec_2_pre_target(1) := bru.pre_target_M
    stage_fec_2_pre_target(2) := bru.pre_target_R

    stage_fec_2_hascode(0)  :=  Mux(stage2_flush,0.U,Mux(stage2_stall,Hash(bru.pc(19,4)),stage_fec_2_hascode(0)))
    stage_fec_2_hascode(1)  :=  Mux(stage2_flush,0.U,Mux(stage2_stall,Hash(bru.pc(19,4)),stage_fec_2_hascode(1)))
    stage_fec_2_hascode(2)  :=  Mux(stage2_flush,0.U,Mux(stage2_stall,Hash(bru.pc(19,4)),stage_fec_2_hascode(2)))

    val stage_fec_2_stall_reg = RegInit(0.U.asBool)
    stage_fec_2_stall_reg := stage_fec_2_stall
//关闭分支预测
    stage_fec_2_branch_answer := commit_bru_reg && bru.pre_L.asBool && (stage_fec_2_inst_branch || stage_fec_2_inst_jump) && bru.btb_hit(0) && stage_fec_2_stall_reg && stage_fec_2_valid && !_cp0.io.exception.asBool
    ready_to_branch_pre := stage_fec_2_branch_answer

    // val stage_fec_2_branch_answer_reg = RegInit(0.U.asBool)
    //stage_fec_2_branch_answer_reg := Mux(stage_fec_2_stall_reg,stage_fec_2_branch_answer,stage_fec_2_branch_answer_reg)
    val true_branch_state = stage_fec_2_branch_answer //Mux(stage_fec_2_stall_reg,stage_fec_2_branch_answer,stage_fec_2_branch_answer_reg)

    stage_fec_2_branch_target  := stage_fec_2_pre_target(0)
    

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

    inst_buffer.write_en    := Mux(stage_fec_2_data_valid || 1.U.asBool,inst_write_en,0.U)
    inst_buffer.write_in(0) := Cat(inst_tlb_exception,jr_Decoder(inst_rdata_L(31,0)),inst_rdata_L(39,32),bru.pht_out,true_branch_state,stage_fec_2_bht(0),stage_fec_2_pht(0),stage_fec_2_pre_target(0),stage_fec_2_hascode(0),stage_fec_2_lookup_data(0),inst_rdata_L(31,0),stage_fec_2_pc_L.io_out.pc_value_out)
    inst_buffer.write_in(1) := inst_buffer.write_in(0)//Cat(jr_Decoder(inst_rdata_M(31,0)),inst_rdata_M(39,32),bru.pht_out,true_branch_state,stage_fec_2_bht(1),stage_fec_2_pht(1),stage_fec_2_pre_target(1),stage_fec_2_hascode(1),stage_fec_2_lookup_data(1),inst_rdata_M(31,0),stage_fec_2_pc_M.io_out.pc_value_out)
    inst_buffer.write_in(2) := inst_buffer.write_in(0)//Cat(jr_Decoder(inst_rdata_R(31,0)),inst_rdata_R(39,32),bru.pht_out,true_branch_state,stage_fec_2_bht(2),stage_fec_2_pht(2),stage_fec_2_pre_target(2),stage_fec_2_hascode(2),stage_fec_2_lookup_data(2),inst_rdata_R(31,0),stage_fec_2_pc_R.io_out.pc_value_out)
    inst_buffer.read_en     := Mux(_cfu.io.StallF.asBool,1.U,0.U) //继续流水线流下去，就发射一条指令
    inst_buffer.point_flush := _cp0.io.exception


    stage2_flush := (inst_buffer.point_write_en && !inst_buffer.empty) || _cp0.io.exception.asBool


    _cfu.io.Inst_Fifo_Empty := inst_buffer.empty
    _pre_cfu.io.fifo_full := inst_buffer.full
    _pre_cfu.io.data_ok   := inst_data_ok
    // _pre_cfu.io.stage_fec_2_req := stage_fec_2_req
    _pre_cfu.io.stage2_stall := stage2_stall
    //同时也在写入，就不需要将上一个指令写入进行修改
    when(inst_buffer.point_write_en.asBool && inst_buffer.empty.asBool && inst_write_en === 0.U) {
        stage1_valid_flush := 2.U
    }.elsewhen(((inst_buffer.point_write_en.asBool && (!inst_buffer.empty.asBool || (inst_buffer.empty.asBool && inst_write_en =/= 0.U))) || inst_buffer.point_flush.asBool) ){//此时fifo写入的任何数据都无用
        stage1_valid_flush := 1.U
    }.otherwise{
         stage1_valid_flush := 0.U
    }
    ready_to_branch := inst_buffer.point_write_en
    inst_buffer_full := inst_buffer.full



    pre_decoder_branchD_flag := Mux(_cfu.io.FlushD.asBool,0.U,Mux(_cfu.io.StallD.asBool,inst_buffer.read_out(0)(125),pre_decoder_branchD_flag))
    pre_decoder_jump := Mux(_cfu.io.FlushD.asBool,0.U,Mux(_cfu.io.StallD.asBool,inst_buffer.read_out(0)(126),pre_decoder_jump))
    pre_decoder_branchdata := Mux(_cfu.io.FlushD.asBool,0.U,Mux(_cfu.io.StallD.asBool,inst_buffer.read_out(0)(132,127),pre_decoder_branchdata))
    pre_decoder_jr := Mux(_cfu.io.FlushD.asBool,0.U,Mux(_cfu.io.StallD.asBool,inst_buffer.read_out(0)(133),pre_decoder_jr))
    
//full是用来控制前面流水线停止的，和这里无关
    val InDelaySlotF = RegInit(0.U.asBool)
    when((pre_decoder_branchD_flag.asBool || pre_decoder_jump.asBool) && !_cfu.io.StallF ) {
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
    _if2id.io.PCPlus4F       := _if2id.io.PCF  + 4.U
    _if2id.io.PCPlus8F       := _if2id.io.PCF  + 8.U
    _if2id.io.en             := _cfu.io.StallD //相当于清空ID阶段的所有数据
    _if2id.io.clr            := _cfu.io.FlushD
    _if2id.io.ExceptionTypeF := inst_buffer.read_out(0)(135,134)
   
    _if2id.io.InstrF      := Mux(_cu.io1.BadInstrD.asBool || _cu.io1.SysCallD.asBool||_cu.io1.BreakD.asBool||
        _cu.io1.EretD.asBool,0.U,inst_buffer.read_out(0)(63,32))//如果延迟曹指令要跳转的话，就直接不运行这一条指令，避免对后面的操作产生影响
 
    _if2id.io.NextDelaySlotD := (pre_decoder_branchD_flag.asBool || pre_decoder_jump.asBool) || InDelaySlotF //此时指令对齐才有可能为延迟曹


val id_exception = RegInit(0.U.asBool)
id_exception := Mux(_cfu.io.FlushD.asBool,0.U,Mux(_cfu.io.StallD.asBool,inst_buffer.read_out(0)(1,0) =/= 0.U || inst_buffer.read_out(0)(135,134) =/= 0.U,id_exception))

val ex_exception = RegInit(0.U.asBool)
ex_exception := Mux(_cfu.io.FlushE.asBool,0.U,Mux(_cfu.io.StallE.asBool,_id2ex.io.ExceptionTypeD =/= 0.U,ex_exception))

val mem_exception = RegInit(0.U.asBool)
mem_exception := Mux(_cfu.io.FlushM.asBool,0.U,Mux(_cfu.io.StallM.asBool,_ex2mem.io.ExceptionTypeE =/= 0.U,mem_exception))

val mem2_exception = RegInit(0.U.asBool)
mem2_exception := Mux(_cfu.io.FlushM2.asBool,0.U,Mux(_cfu.io.StallM2.asBool,_mem2mem2.io.ExceptionTypeE =/= 0.U,mem2_exception))

val wb_exception = RegInit(0.U.asBool)
wb_exception := Mux(_cfu.io.FlushW.asBool,0.U,Mux(_cfu.io.StallW.asBool,_mem22wb.io.ExceptionTypeM =/= 0.U,wb_exception))
//======================================
//下面定义流水线每一级的bru状态信息

class bru_bundle extends Bundle {
    val pht = Input(UInt(2.W))
    val bht = Input(UInt(7.W))
    val hashcode = Input(UInt(4.W))
    val target_pc = Input(UInt(32.W))
    val lookup_data = Input(UInt(7.W))
    val pht_lookup_value = Input(UInt(8.W))
    // val pc_valid_In = Input(Bool())
}

class bru_detail extends Module {
    val io = IO(new Bundle{
        val stall = Input(Bool())
        val flush = Input(Bool())   
    })
    val io_in = IO(new bru_bundle)
    val io_out = IO(Flipped(new bru_bundle))

      val pht_value = RegInit(0.U(2.W))//RegInit(Cat(0xbfbf.U,0xfffc.U))
      val bht_value = RegInit(0.U(7.W))
      val hashcode_value = RegInit(0.U(4.W))
      val target_pc_value = RegInit(0.U(32.W))
      val lookup_data_value = RegInit(0.U(7.W))
      val pht_lookup_value_data = RegInit(0.U(8.W))
    pht_value := Mux(io.flush,0.U,Mux(io.stall,io_in.pht,pht_value))
    bht_value := Mux(io.flush,0.U,Mux(io.stall,io_in.bht,bht_value))
    hashcode_value := Mux(io.flush,0.U,Mux(io.stall,io_in.hashcode,hashcode_value))
    target_pc_value := Mux(io.flush,0.U,Mux(io.stall,io_in.target_pc,target_pc_value))
    lookup_data_value := Mux(io.flush,0.U,Mux(io.stall,io_in.lookup_data,lookup_data_value))
    pht_lookup_value_data := Mux(io.flush,0.U,Mux(io.stall,io_in.pht_lookup_value,pht_lookup_value_data))


    io_out.pht := pht_value
    io_out.bht := bht_value
    io_out.hashcode := hashcode_value
    io_out.target_pc := target_pc_value
    io_out.lookup_data := lookup_data_value
    io_out.pht_lookup_value := pht_lookup_value_data
    //pc_value := Mux(reset.asBool,Cat(0xbfbf.U,0xfff4.U),Mux(io.flush,0.U,Mux(io.stall,io_in.pc_value_in,pc_value)))
  
}

val id_true_branch_state = RegInit(0.U.asBool)
id_true_branch_state := Mux(_cfu.io.FlushD.asBool,0.U,Mux(_cfu.io.StallD.asBool,inst_buffer.read_out(0)(116),id_true_branch_state))

val id_bru_state =  Module(new bru_detail)
id_bru_state.io.flush := _cfu.io.FlushD
id_bru_state.io.stall := _cfu.io.StallD

val ex_bru_state =  Module(new bru_detail)
ex_bru_state.io.flush := _cfu.io.FlushE
ex_bru_state.io.stall := _cfu.io.StallE

val mem_bru_state =  Module(new bru_detail)
mem_bru_state.io.flush := _cfu.io.FlushM
mem_bru_state.io.stall := _cfu.io.StallM

val mem2_bru_state =  Module(new bru_detail)
mem2_bru_state.io.flush := _cfu.io.FlushM2
mem2_bru_state.io.stall := _cfu.io.StallM2

val wb_bru_state =  Module(new bru_detail)
wb_bru_state.io.flush := _cfu.io.FlushW
wb_bru_state.io.stall := _cfu.io.StallW

//ex_bru_state.io_in <> id_bru_state.io_out
mem_bru_state.io_in  <> ex_bru_state.io_out
mem2_bru_state.io_in <> mem_bru_state.io_out
wb_bru_state.io_in   <> mem2_bru_state.io_out
//===========================================
    
//-------------ID----------
val inst_tlb_exceptionE = RegInit(0.U.asBool)
inst_tlb_exceptionE := Mux(_cfu.io.FlushE.asBool,0.U,Mux(_cfu.io.StallE.asBool,_if2id.io.ExceptionTypeD_Out =/= 0.U,inst_tlb_exceptionE))

    id_bru_state.io_in.lookup_data := inst_buffer.read_out(0)(70,64)
    id_bru_state.io_in.hashcode := inst_buffer.read_out(0)(74,71)
    id_bru_state.io_in.target_pc := inst_buffer.read_out(0)(106,75)
    id_bru_state.io_in.pht := inst_buffer.read_out(0)(108,107)
    id_bru_state.io_in.bht := inst_buffer.read_out(0)(115,109)
    id_bru_state.io_in.pht_lookup_value := inst_buffer.read_out(0)(124,117)

    val branch_stateD = id_bru_state.io_out.pht(1) //根据状态机的意义，第一位代表有没有发生分支跳转



    val target_neq_branchD = ( id_bru_state.io_out.target_pc =/= PCBranchD ) 
    val target_neq_jumpD   = ( id_bru_state.io_out.target_pc =/= PCJumpD )

    val target_addr_error = (pre_decoder_jump.asBool && target_neq_jumpD) || (_br.io.exe.asBool && target_neq_branchD)

    //这一块感觉有点问题
    inst_buffer.point_write_en := _cfu.io.StallD.asBool &&( ((pre_decoder_jump.asBool  || _br.io.exe.asBool ) =/= id_true_branch_state) || target_addr_error)

    val PC_nextD = MuxCase((_if2id.io.PCPlus8D ),Seq(
        pre_decoder_jump.asBool -> PCJumpD,
        _br.io.exe.asBool   -> PCBranchD
    ))
    val Pc_targetD = MuxCase((0.U ),Seq(
        pre_decoder_branchD_flag.asBool -> PCBranchD,
        pre_decoder_jump.asBool -> PCJumpD
    ))
    val true_branch_stateE = RegInit(0.U.asBool)
    
    true_branch_stateE := Mux(_cfu.io.FlushE.asBool,0.U ,Mux(_cfu.io.StallE.asBool,(pre_decoder_jump.asBool || _br.io.exe.asBool),true_branch_stateE))

    ex_bru_state.io_in.target_pc :=  Pc_targetD
    ex_bru_state.io_in.bht := id_bru_state.io_out.bht
    ex_bru_state.io_in.pht := id_bru_state.io_out.pht
    ex_bru_state.io_in.hashcode := id_bru_state.io_out.hashcode
    ex_bru_state.io_in.lookup_data := id_bru_state.io_out.lookup_data
    ex_bru_state.io_in.pht_lookup_value := id_bru_state.io_out.pht_lookup_value

    val bht_tobeE = Cat(ex_bru_state.io_out.bht(5,0),true_branch_stateE)
    val pht_tobeE = br_state_machine_next_state(ex_bru_state.io_out.pht,true_branch_stateE)
    val pht_lookup_value_tobeE = MuxLookup(ex_bru_state.io_out.lookup_data(1,0),Cat(ex_bru_state.io_out.pht_lookup_value(7,2),pht_tobeE),Seq(
        1.U -> Cat(ex_bru_state.io_out.pht_lookup_value(7,4),pht_tobeE,ex_bru_state.io_out.pht_lookup_value(1,0)),
        2.U -> Cat(ex_bru_state.io_out.pht_lookup_value(7,6),pht_tobeE,ex_bru_state.io_out.pht_lookup_value(3,0)),
        3.U -> Cat(pht_tobeE,ex_bru_state.io_out.pht_lookup_value(5,0))
    ))
    mem_bru_state.io_in.pht_lookup_value := pht_lookup_value_tobeE
    mem_bru_state.io_in.bht := bht_tobeE
    mem_bru_state.io_in.pht := pht_tobeE

    Pc_Next_normal := Mux(inst_buffer.point_write_en,PC_nextD,stage_fec_1_pc_next)


    val BadVAddrD = _if2id.io.PCD 
    _regfile.io.A1 := InstrD(25,21)
    _regfile.io.A2 := InstrD(20,16) 
    _cu.io1.InstrD := _if2id.io.InstrD
    _br.io.rs := BranchRsD
    BranchRsD:= Mux(_cfu.io.ForwardAD(0),Forward_ResultM,Mux(_cfu.io.ForwardAD(1),Forward_ResultM2,_regfile.io.RD1))//前递，降低流水线阻塞,后面是指需要读寄存器的时候的值
    _br.io.rt := BranchRtD
    BranchRtD:= Mux(_cfu.io.ForwardBD(0),Forward_ResultM,Mux(_cfu.io.ForwardBD(1),Forward_ResultM2,_regfile.io.RD2))
    ExceptionTypeD_Out := Mux1H(Seq(
        (_if2id.io.PCD(1,0) =/= 0.U) -> EXCEP_MASK_AdELI ,
        (_if2id.io.ExceptionTypeD_Out(0)) -> EXCEP_MASK_TLBRefill_L,
        (_if2id.io.ExceptionTypeD_Out(1)) -> EXCEP_MASK_TLBInvalid_L 
    ))
    _br.io.en := 1.U.asBool//Mux(((id_exception | ex_exception |mem_exception | mem2_exception | wb_exception) === 0.U),1.U,0.U) //不允许触发分支//有例外的话
    _br.io.branch := pre_decoder_branchdata

   // val int_instanceD  = RegInit(0.U(6.W))
    val int_instanceE  = RegInit(0.U(6.W))
    val int_instanceM  = RegInit(0.U(6.W))    
    val int_instanceM2 = RegInit(0.U(6.W))
    val int_instanceW  = RegInit(0.U(6.W))

    val int_with_timer_int = Cat(_cp0.io.timer_int_has || ext_int(5),ext_int(4,0)) 

    int_instanceE := Mux(_cfu.io.FlushE.asBool,0.U,Mux(_cfu.io.StallE.asBool,int_with_timer_int,int_instanceE))
    int_instanceM := Mux(_cfu.io.FlushM.asBool,0.U,Mux(_cfu.io.StallM.asBool,int_instanceE,int_instanceM))
    int_instanceM2 := Mux(_cfu.io.FlushM2.asBool,0.U,Mux(_cfu.io.StallM2.asBool,int_instanceM,int_instanceM2))
    int_instanceW := Mux(_cfu.io.FlushW.asBool,0.U,Mux(_cfu.io.StallW.asBool,int_instanceM2,int_instanceW))


  

    
    _id2ex.io.ExceptionTypeD  := Mux(((int_with_timer_int & _cp0.io.cp0_status) =/= 0.U) && _cp0.io.Int_able.asBool,EXCEP_MASK_INT,Mux( ExceptionTypeD_Out === 0.U,(
        (Mux(_cu.io1.BadInstrD.asBool,EXCEP_MASK_RI,0.U)) | 
        (Mux(_cu.io1.SysCallD.asBool,EXCEP_MASK_Sys,0.U)) |
        (Mux(_cu.io1.BreakD.asBool,EXCEP_MASK_Bp,0.U))    |
        (Mux(_cu.io1.EretD.asBool,EXCEP_MASK_ERET,0.U)) ),ExceptionTypeD_Out))

    _id2ex.io.RsD := RsD
    _id2ex.io.RtD := RtD 
    _id2ex.io.RdD := RdD 
    _id2ex.io.ImmD:= ImmD
    _id2ex.io.RD1D:= Mux(_cfu.io.ForwardAD(0),Forward_ResultM,Mux(_cfu.io.ForwardAD(1),Forward_ResultM2,_regfile.io.RD1))
    _id2ex.io.RD2D:= Mux(_cfu.io.ForwardBD(0),Forward_ResultM,Mux(_cfu.io.ForwardBD(1),Forward_ResultM2,_regfile.io.RD2))
    _id2ex.io.WriteCP0AddrD := Mux(_cu.io1.Tlb_Control(2),"b00000".U,InstrD(15,11))
    _id2ex.io.WriteCP0SelD  := Mux(_cu.io1.Tlb_Control(2),0.U,InstrD(2,0))
    _id2ex.io.ReadCP0AddrD  := MuxCase(InstrD(15,11),Seq(
        _cu.io1.EretD.asBool     -> "b01110".U ,
        _cu.io1.Tlb_Control(2)   -> "b01010".U ,
        _cu.io1.Tlb_Control(1)   -> "b00000".U ,
        _cu.io1.Tlb_Control(0)   -> "b00000".U
    ))
        // Mux(_cu.io1.EretD.asBool,"b01110".U , Mux(_cu.io1.Tlb_Control(2),"b01010".U,Mux(InstrD(15,11)))
    _id2ex.io.ReadCP0SelD   := Mux(_cu.io1.EretD.asBool || _cu.io1.Tlb_Control(2) || _cu.io1.Tlb_Control(1) || _cu.io1.Tlb_Control(0),0.U,InstrD(2,0))
    _id2ex.io.PCPlus8D      := PCPLus8D
    _id2ex.io.InDelaySlotD := _if2id.io.InDelaySlotD.asBool && (_if2id.io.PCD(1,0) === 0.U)
    _id2ex.io.PCD          := _if2id.io.PCD
    //说明这一条指令为分支或者间接跳转指令
    _id2ex.io.BranchJump_JrD  := Cat(0.U(1.W),pre_decoder_branchD_flag.asBool || pre_decoder_jump.asBool)
    _id2ex.io.BadVaddrD    := BadVAddrD
    _id2ex.io.Tlb_Control  := _cu.io1.Tlb_Control

 //&& !inst_buffer.empty
    _pre_cfu.io.branch_error := inst_buffer.point_write_en//(_cu.io.JumpD.asBool || _br.io.exe.asBool) //&& _cfu.io.StallD.asBool

    _cfu.io.dmem_calD := _cu.io1.dmem_addr_cal

    

//-------------EX----------

val inst_tlb_exceptionM = RegInit(0.U.asBool)
inst_tlb_exceptionM := Mux(_cfu.io.FlushM.asBool,0.U,Mux(_cfu.io.StallM.asBool,inst_tlb_exceptionE,inst_tlb_exceptionM))
    //前递还没写
    val RD1ForWardE_p   = Mux2_4(_cfu.io.ForwardAE,_id2ex.io.RD1E,ResultW,Forward_ResultM,Forward_ResultM2)//rd1为rs对应的
    val RD2ForWardE_p   = Mux2_4(_cfu.io.ForwardBE,_id2ex.io.RD2E,ResultW,Forward_ResultM,Forward_ResultM2) //rd2为rt对应
    val RD1ForWardE_r   = RegInit(0.U(32.W))  
    val RD2ForWardE_r   = RegInit(0.U(32.W))
    val Forward_Lock1E  = RegInit(0.U(1.W)) 
    val Forward_Lock2E  = RegInit(0.U(1.W))
    val Forward_CP0_data = MuxLookup(_cfu.io.ForwardCP0E, _cp0.io.cp0_read_data,Seq(
            "b01".U -> _ex2mem.io.WriteCP0HiLoDataM,
            "b10".U -> _mem2mem2.io.WriteCP0HiLoDataM
        ))//_cfu.io.ForwardCP0E === "b01".U,_ex2mem.io.WriteCP0HiLoDataM, _cp0.io.cp0_read_data)
    RD1ForWardE := Mux(Forward_Lock1E.asBool,RD1ForWardE_r,RD1ForWardE_p)
    RD2ForWardE := Mux(Forward_Lock2E.asBool,RD2ForWardE_r,RD2ForWardE_p)
    when(_cfu.io.StallE.asBool) {
        Forward_Lock1E := 0.U
        Forward_Lock2E := 0.U
    }.otherwise {
        when((_cfu.io.ForwardAE(0) ||_cfu.io.ForwardAE(1) ) && !Forward_Lock1E.asBool) {//Rs,并且01表示ex阶段的寄存器与现在处于wb阶段的指令需要写入的指令相同
            Forward_Lock1E := !(_ex2mem.io.MemToRegM.asBool || _mem2mem2.io.MemToRegM.asBool)    //而写回阶段是知道了寄存器值应该是多少的，直接前递回来
            RD1ForWardE_r  := RD1ForWardE_p
        }
        when((_cfu.io.ForwardBE(0) || _cfu.io.ForwardBE(1)) && !Forward_Lock2E.asBool){//Rs,并且01表示ex阶段的寄存器与现在处于wb阶段的指令需要写入的指令相同
            Forward_Lock2E := !(_ex2mem.io.MemToRegM.asBool || _mem2mem2.io.MemToRegM.asBool) 
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
    
    _muldiv.io.en := !ex_exception//Mux(_id2ex.io.ExceptionTypeE_Out =/= 0.U , 0.U,1.U)
    _muldiv.io.in1 := Src1E
    _muldiv.io.in2 := Src2E
    _muldiv.io.ctrl := Cat(_id2ex.io2.ALUCtrlE(ALU_MUL),_id2ex.io2.ALUCtrlE(9,8),_id2ex.io2.ALUCtrlE(6,5))

    // val dmemreq_start =( _id2ex.io2.MemRLE =/= 0.U) && (_id2ex.io2.MemWriteE.asBool)//需要写寄存器并且那个煞笔rl不等于零，便说明出现了问题

    val HiInE = Mux(_id2ex.io2.HiLoWriteE === "b10".U,WriteCP0HiLoDataE,_muldiv.io.hi) // b10 对应MTHI指令，通用寄存器到HI寄存器
    val LoInE = Mux(_id2ex.io2.HiLoWriteE === "b01".U,WriteCP0HiLoDataE,_muldiv.io.lo) // b10 对应MTHI指令，通用寄存器到HI寄存器

    
    
    _dmemreq.io.en := Mux((ex_exception || mem_exception || mem2_exception || wb_exception
       ) /*||Exception_state.asBool*/,0.U,((!_addr_cal.io.d_unaligned.asBool && _cfu.io.StallE.asBool) || mem_write)) // 后面几级流水线没有出现例外，加上流水线没有停止，
        // 再加上有mem到reg的操作或者写Mem的操作，才会有所谓的mem access，其实就是使能memreq
    _dmemreq.io.MemToRegE := _id2ex.io2.MemToRegE
    _dmemreq.io.MemWidthE := _id2ex.io2.MemWidthE
    _dmemreq.io.VAddrE  := _addr_cal.io.d_paddr
    _dmemreq.io.MemWriteE := _id2ex.io2.MemWriteE
    // _dmemreq.io.addr_ok   := io.data_addr_ok
    _dmemreq.io.addr_ok   := data_addr_ok
    _dmemreq.io.WriteDataE := WriteDataE
    _dmemreq.io.memrl := _id2ex.io2.MemRLE


    //读寄存器HI 和 LO
    _ex2mem.io.HiLoOutE := Mux1H(Seq(
        _id2ex.io2.HiLoToRegE(0) -> Mux2_4(_cfu.io.ForwardHE,_hilo.io.lo_o,LoInW,LoInM ,_mem2mem2.io.LoInM),
        _id2ex.io2.HiLoToRegE(1) -> Mux2_4(_cfu.io.ForwardHE,_hilo.io.hi_o,HiInW,HiInM ,_mem2mem2.io.HiInM)
    ))
    // val d_addr_unaligned = 
    val temp_exceptionE = Mux(_id2ex.io.ExceptionTypeE_Out =/= 0.U,_id2ex.io.ExceptionTypeE_Out,
        (Mux(_addr_cal.io.d_unaligned.asBool && _id2ex.io2.MemToRegE.asBool ,EXCEP_MASK_AdELD,0.U)) |
        (Mux(_addr_cal.io.d_unaligned.asBool && _id2ex.io2.MemWriteE.asBool ,EXCEP_MASK_AdES,0.U))  |
        (Mux(_alu.io.overflow.asBool,EXCEP_MASK_Ov,0.U))) //取或运算便于搞事,之后要多减少位数，感觉32位稍微有点长了

    val cp0_epc_data = _cp0.io.epc


    val Forward_for_epc = Mux(_ex2mem.io.CP0WriteM.asBool && _ex2mem.io.WriteCP0AddrM === "b01110".U,_ex2mem.io.WriteCP0HiLoDataM,Mux(
        _mem2mem2.io.CP0WriteM.asBool && _mem2mem2.io.WriteCP0AddrM === "b01110".U,_mem2mem2.io.WriteCP0HiLoDataM,cp0_epc_data))

    BadVAddrE:= Mux(_addr_cal.io.d_unaligned.asBool&& (_id2ex.io2.MemToRegE.asBool || _id2ex.io2.MemWriteE.asBool) ,_addr_cal.io.d_vaddr,Mux(_id2ex.io.ExceptionTypeE_Out(31) && Forward_for_epc(1,0) =/= 0.U,Forward_for_epc,Inst_badvaddrE)) //mmu模块发现数据指令错位,下面还需要加上数据错误例外 直接加法计算

    

    _ex2mem.io.ExceptionTypeE  :=  Mux(_id2ex.io.ExceptionTypeE_Out(31) && Forward_for_epc(1,0) =/= 0.U,EXCEP_MASK_AdELI,0.U) | temp_exceptionE
    
    _ex2mem.io.ReadCP0DataE :=  Forward_CP0_data//cp0的读取数据，加上了前di
    _ex2mem.io.CP0WriteE    := _id2ex.io2.CP0WriteE
    _ex2mem.io.ALUOutE := _alu.io.result
    _ex2mem.io.BadVAddrE := BadVAddrE
    _ex2mem.io.HiInE    := HiInE
    _ex2mem.io.LoInE    := LoInE
    _ex2mem.io.WriteRegE := WriteRegE//要写的寄存器
    _ex2mem.io.WriteDataE := WriteDataE
    _ex2mem.io.WriteCP0HiLoDataE := WriteCP0HiLoDataE
    _ex2mem.io.PhyAddrE   := _addr_cal.io.d_paddr
    _ex2mem.io.RtE        := RD2ForWardE



    _cp0.io.cp0_read_addr := _id2ex.io2.ReadCP0AddrE
    _cp0.io.cp0_read_sel := _id2ex.io2.ReadCP0SelE
    _cp0.io.cp0_write_addr := _mem22wb.io.WriteCP0AddrW
    _cp0.io.cp0_write_sel :=_mem22wb.io.WriteCP0SelW
    _cp0.io.cp0_write_data := _mem22wb.io.WriteCP0HiLoDataW

    _hilo.io.hi_i := _mem22wb.io.HiInW
    _hilo.io.lo_i := _mem22wb.io.LoInW//写回阶段，将寄存器的值写回

    val resultE = MuxCase(_alu.io.result,Seq(
        CP0ToRegE.asBool -> Forward_CP0_data,
       ( _id2ex.io2.HiLoToRegE =/= 0.U ) -> _ex2mem.io.HiLoOutE,
       _id2ex.io2.LinkE.asBool -> _id2ex.io2.PCPlus8E,
       _id2ex.io2.ALUCtrlE(ALU_MUL) -> _muldiv.io.lo
    ))

    val resultE2M_Reg = RegInit(0.U(32.W))
    //val not_alu_reg = RegInit(0.U.asBool)
   // val not_alu_out = CP0ToRegE.asBool || ( _id2ex.io2.HiLoToRegE =/= 0.U ) || _id2ex.io2.LinkE.asBool
    resultE2M_Reg := Mux(_cfu.io.FlushM.asBool,0.U,Mux(_cfu.io.StallM.asBool,resultE,resultE2M_Reg))
   // not_alu_reg := Mux(_cfu.io.FlushM.asBool,0.U,Mux(_cfu.io.StallM.asBool,not_alu_out,not_alu_reg))
   
    


// 钢架的
// 钢架的

// ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！

//-------------MEM1-----------

class tlb_data_register extends Module with mips_macros{
    val io = IO(new Bundle { 
    val      flush  = Input(Bool()) 
    val      stall  = Input(Bool())   
    val      tlb_read_data  = Flipped(new tlb_write_or_read_port)
    val      tlb_write_data = new tlb_write_or_read_port

    })
    val tlb_data_reg = RegInit(0.U(78.W))
    tlb_data_reg := Mux(io.flush,0.U,Mux(io.stall,io.tlb_write_data.tlb_port_to_UInt,tlb_data_reg))
    io.tlb_read_data.v(1)     := get_cdv1(tlb_data_reg)(0)
    io.tlb_read_data.d(1)     := get_cdv1(tlb_data_reg)(1)
    io.tlb_read_data.c(1)     := get_cdv1(tlb_data_reg)(4,2)
    io.tlb_read_data.v(0)     := get_cdv0(tlb_data_reg)(0)
    io.tlb_read_data.d(0)     := get_cdv0(tlb_data_reg)(1)
    io.tlb_read_data.c(0)     := get_cdv0(tlb_data_reg)(4,2)
    io.tlb_read_data.paddr(0) := get_pfn0(tlb_data_reg)
    io.tlb_read_data.paddr(1) := get_pfn1(tlb_data_reg)
    io.tlb_read_data.vaddr    := get_vpn2(tlb_data_reg)
    io.tlb_read_data.g        := get_g(tlb_data_reg)
    io.tlb_read_data.asid     := get_asid(tlb_data_reg)
}
// module  _ex2mem
val index_search_hitM2 = RegInit(0.U.asBool)
val index_search_indexM2 = RegInit(0.U(19.W))
val tlb_searched_index_value = Wire(UInt(32.W))
Forward_ResultM  :=  resultE2M_Reg
val tlb_read_dataM2  = Module(new tlb_data_register).io
tlb_read_dataM2.flush := _cfu.io.FlushM2
tlb_read_dataM2.stall := _cfu.io.StallM2
tlb_read_dataM2.tlb_write_data := cp0_tlb_write_data


tlbp_search_vaddr := Cat(Forward_ResultM(31,13),0.U(13.W))
tlb_read_index    := Forward_ResultM(tlb_index_width - 1,0)




//tlb_control(2) 代表为tlbp指令
tlbp_search_en    := _ex2mem.io.Tlb_ControlM(2)
//hit，第32位为0，不命中，第32位为1
tlb_searched_index_value := Cat(!tlb_search_hit,0.U((32 - tlb_index_width - 1).W),tlb_search_index)

val ResultM2_Reg  =  RegInit(0.U(32.W))
val Cal_ResultM  =  Forward_ResultM
ResultM2_Reg  :=    Mux(_cfu.io.FlushM2.asBool,0.U,Mux(_cfu.io.StallM2.asBool,Cal_ResultM,ResultM2_Reg))

val ExceptionM = Mux(mem_exception,_ex2mem.io.ExceptionTypeM_Out,Mux1H(Seq(
    data_tlb_exception(0) -> Mux(_ex2mem.io.MemToRegM.asBool,EXCEP_MASK_TLBRefill_L,EXCEP_MASK_TLBRefill_S),
    data_tlb_exception(1) -> Mux(_ex2mem.io.MemToRegM.asBool,EXCEP_MASK_TLBInvalid_L,EXCEP_MASK_TLBInvalid_S),
    data_tlb_exception(2) -> EXCEP_MASK_TLBModified
)))

_mem2mem2.io.WriteRegE := _ex2mem.io.WriteRegM
_mem2mem2.io.PhyAddrE := _ex2mem.io.PhyAddrM
_mem2mem2.io.HiInE := _ex2mem.io.HiInM
_mem2mem2.io.LoInE := _ex2mem.io.LoInM
_mem2mem2.io.CP0WriteE := _ex2mem.io.CP0WriteM.asBool || _ex2mem.io.Tlb_ControlM(2)

_mem2mem2.io.WriteCP0HiLoDataE := MuxCase( _ex2mem.io.WriteCP0HiLoDataM,Seq(
    (data_tlb_exception =/= 0.U) -> Cat(_ex2mem.io.PhyAddrM(31,13),0.U(5.W),_cp0.io.asid),
    _ex2mem.io.Tlb_ControlM(2)   -> tlb_searched_index_value,
    inst_tlb_exceptionM          -> Cat(_ex2mem.io.PCM(31,13),0.U(5.W),_cp0.io.asid)
))
    // (data_tlb_exception =/= 0.U ,Cat(_ex2mem.io.PhyAddrM(31,13),0.U(5.W),_cp0.io.asid),Mux(_ex2mem.io.Tlb_ControlM(2),tlb_searched_index_value, _ex2mem.io.WriteCP0HiLoDataM))

_mem2mem2.io.ExceptionTypeE := ExceptionM//_ex2mem.io.ExceptionTypeM_Out
_mem2mem2.io.RtE := _ex2mem.io.RtM

_mem2mem2.io1.RegWriteE      := _ex2mem.io.RegWriteM
_mem2mem2.io1.MemToRegE      := _ex2mem.io.MemToRegM
_mem2mem2.io1.MemWriteE      := _ex2mem.io.MemWriteM

_mem2mem2.io1.PCPlus8E       := _ex2mem.io.PCPlus8M
_mem2mem2.io1.LoadUnsignedE  := _ex2mem.io.LoadUnsignedM
_mem2mem2.io1.MemWidthE      := _ex2mem.io.MemWidthM
_mem2mem2.io1.HiLoWriteE     := _ex2mem.io.HiLoWriteM

_mem2mem2.io1.CP0WriteE      := _ex2mem.io.CP0WriteM.asBool || _ex2mem.io.Tlb_ControlM(2) || data_tlb_exception =/= 0.U
_mem2mem2.io1.WriteCP0AddrE  := Mux(data_tlb_exception =/= 0.U || inst_tlb_exceptionM,"b01010".U,_ex2mem.io.WriteCP0AddrM)
_mem2mem2.io1.WriteCP0SelE   := Mux(data_tlb_exception =/= 0.U || inst_tlb_exceptionM ,0.U,_ex2mem.io.WriteCP0SelM)
_mem2mem2.io1.PCE            := _ex2mem.io.PCM
_mem2mem2.io1.InDelaySlotE   := _ex2mem.io.InDelaySlotM
_mem2mem2.io1.MemRLE         := _ex2mem.io.MemRLM
_mem2mem2.io1.BranchJump_JrE := _ex2mem.io.BranchJump_JrM
_mem2mem2.io1.HiLoToRegE     := _ex2mem.io.HiLoToRegM
_mem2mem2.io1.LinkE          := _ex2mem.io.LinkM
_mem2mem2.io.ReadCP0DataE    := _ex2mem.io.ReadCP0DataM
_mem2mem2.io1.ALUCtrlE   := 0.U
_mem2mem2.io1.ALUSrcE    := 0.U
_mem2mem2.io1.ReadCP0SelE  := 0.U
_mem2mem2.io1.ReadCP0AddrE := 0.U

_mem2mem2.io.CP0ToRegE  := _ex2mem.io.CP0ToRegM
_mem2mem2.io1.Tlb_Control := _ex2mem.io.Tlb_ControlM

val tlb_exception_cp0_writeM2 = RegInit(0.U.asBool)
val tlb_exception_co0_writeW  = RegInit(0.U.asBool)
tlb_exception_cp0_writeM2 := Mux(_cfu.io.FlushM2.asBool,0.U,Mux(_cfu.io.StallM2.asBool,data_tlb_exception =/= 0.U || inst_tlb_exceptionM,tlb_exception_cp0_writeM2))
tlb_exception_co0_writeW  := Mux(_cfu.io.FlushW.asBool,0.U,Mux(_cfu.io.StallW.asBool,tlb_exception_cp0_writeM2,tlb_exception_co0_writeW))




_mem2mem2.io.BadVAddrE       := Mux(data_tlb_exception =/= 0.U,_ex2mem.io.PhyAddrM,Mux(_ex2mem.io.ExceptionTypeM_Out(EXCEP_AdELI) && (_ex2mem.io.BadVAddrM(31) === 0.U 
    || _ex2mem.io.BadVAddrM(31,30) === "b11".U ),_ex2mem.io.PCM,_ex2mem.io.BadVAddrM))
_mem2mem2.io.ALUOutE         := _ex2mem.io.ALUOutM
 _mem2mem2.io1.RegDstE       := 0.U 
 _mem2mem2.io.WriteDataE     := _ex2mem.io.WriteDataM
 _mem2mem2.io.HiLoOutE       := _ex2mem.io.HiLoOutM



 _mem2mem2.io.clr  := _cfu.io.FlushM2
 _mem2mem2.io.en   := _cfu.io.StallM2

//-------------MEM2----------





    val Mem_withRL_Data    = Wire(UInt(32.W)) 
    Forward_ResultM2 := ResultM2_Reg
    val ResultM2   = Mux( _mem2mem2.io.MemToRegM.asBool, Mem_withRL_Data, ResultM2_Reg)
    //   _ex2mem.io.MemToRegM.asBool -> Mem_withRL_Data //刚加的

        // --------------
    Mem_withRL_Data := MuxLookup(_mem2mem2.io.MemRLM,_dmem.io.RD,Seq(
        "b10".U -> MuxLookup(_mem2mem2.io.PhyAddrM(1,0),_dmem.io.RD,Seq(
            "b00".U   -> Cat(_dmem.io.RD(7,0),_mem2mem2.io.RtM(23,0)),
            "b01".U   -> Cat(_dmem.io.RD(15,0),_mem2mem2.io.RtM(15,0)),
            "b10".U   -> Cat(_dmem.io.RD(23,0),_mem2mem2.io.RtM(7,0))    
        )),
        "b01".U  -> MuxLookup(_mem2mem2.io.PhyAddrM(1,0),_dmem.io.RD,Seq(
            "b01".U   -> Cat(_mem2mem2.io.RtM(31,24),_dmem.io.RD(31,8)),
            "b10".U   -> Cat(_mem2mem2.io.RtM(31,16),_dmem.io.RD(31,16)),
            "b11".U   -> Cat(_mem2mem2.io.RtM(31,8),_dmem.io.RD(31,24))  
        ))
    )) 
    _mem22wb.io.ExceptionTypeM  := _mem2mem2.io.ExceptionTypeM_Out


    _mem22wb.io.ResultM         := ResultM2
    _mem22wb.io.BranchJump_JrM  := _mem2mem2.io.BranchJump_JrM


    _mem22wb.io.ReadDataM    :=     Mem_withRL_Data
    _mem22wb.io.BadVAddrM    := _mem2mem2.io.BadVAddrM
 


    // _mem22wb.io.MemToRegM_Forward_hasStall :=&& _ex2mem.io.RegWriteM.asBool &&  _ex2mem.io.MemToRegM.asBool && ((RsD =/= 0.U && RsD === _ex2mem.io.WriteRegM ) || 
    //             (RtD =/= 0.U && RtD ===  _ex2mem.io.WriteRegM &&  _ex2mem.io.RegWriteM.asBool) //mem阶段出现mem2reg并且此时需要前递时，停止流水线
    //             || (_id2ex.io.RsE =/= 0.U && _id2ex.io.RsE === _ex2mem.io.WriteRegM && _id2ex.io2.RegWriteE && _id2ex.io2.Write

    //                 io.ForwardAE := Mux(io.RsE =/= 0.U && io.RsE === io.WriteRegM && io.RegWriteM.asBool,"b10".U,Mux(
    //     io.RsE =/= 0.U && io.RsE === io.WriteRegW && io.RegWriteW.asBool,"b01".U,0.U))

    // io.ForwardBE := Mux(io.RtE =/= 0.U && io.RtE === io.WriteRegM && io.RegWriteM.asBool,"b10".U,Mux(
    //     io.RtE =/= 0.U && io.RtE === io.WriteRegW && io.RegWriteW.asBool,"b01".U,0.U))
    // //防止前面延迟太高，在这里处理地址



//-------------WB----------commit ----  

//module mem22wb
    //ResultW   := Mux(_mem22wb.io.MemToRegW.asBool,_mem22wb.io.ReadDataW,_mem22wb.io.ResultW)
    ResultW   := _mem22wb.io.ResultW//Mux(_mem22wb.io.MemToRegW_Forward_hasStall.asBool,_mem22wb.io.ReadDataW,_mem22wb.io.ResultW)
    RegWriteW := Mux( wb_exception.asBool ,0.U,_mem22wb.io.RegWriteW_Out)
    val HiLoWriteW = Mux(wb_exception/*_mem22wb.io.ExceptionTypeW_Out =/= 0.U*/,0.U,_mem22wb.io.HiLoWriteW) //发生例外，全都清空
    val CP0WriteW  = Mux(wb_exception/*_mem22wb.io.ExceptionTypeW_Out =/= 0.U*/,0.U,_mem22wb.io.CP0WriteW)
    val ExceptionTypeW  = _mem22wb.io.ExceptionTypeW_Out
    // ResultW   := Mux(_mem22wb.io.MemToRegW.asBool,_)
    _regfile.io.WD3 := ResultW
    _regfile.io.A3  := _mem22wb.io.WriteRegW
    _regfile.io.WE3 := RegWriteW

    _hilo.io.we   := HiLoWriteW //没有例外就写寄存器

    tlb_write_en := _mem22wb.io.Tlb_ControlW(0)
    tlb_write_index :=  ResultW(tlb_index_width - 1,0)

   // 提交阶段将数据写回到分支预测的表项中
   bru.bht_in := wb_bru_state.io_out.bht
   bru.pht_in := wb_bru_state.io_out.pht_lookup_value
   bru.bht_write := _mem22wb.io.BranchJump_JrW(0)
   bru.pht_write := bru.bht_write
   bru.btb_write := bru.bht_write
   bru.aw_bht_addr := _mem22wb.io.PCW(10,4)
   bru.aw_pht_addr := wb_bru_state.io_out.lookup_data
   bru.aw_target_addr := wb_bru_state.io_out.target_pc
   bru.aw_pht_ways_addr := wb_bru_state.io_out.hashcode
   bru.write_pc := _mem22wb.io.PCW

    


    // -----------------others that I can not understand
    val disable_cache = RegInit(0.U(1.W))
    disable_cache := Mux((PCF === 0xbfc0005.U<<4 ),1.U,disable_cache)

    //我寻思就一个mmu也没啥好ban的
    _addr_cal.io.d_en := 1.U
        // Mux((_id2ex.io.ExceptionTypeE_Out | _ex2mem.io.ExceptionTypeM_Out | _mem22wb.io.ExceptionTypeW_Out)
        // =/= 0.U,0.U,_cfu.io.StallE & (_id2ex.io2.MemToRegE | _id2ex.io2.MemWriteE))
    _addr_cal.io.d_clr := 0.U  
    _addr_cal.io.d_width := _id2ex.io2.MemWidthE  
    _addr_cal.io.d_vaddr := _id2ex.io.ImmE + _id2ex.io.RD1E//_id2ex.i//RD1ForWardE
    // _addr_cal.io.i_vaddr := _pc2if.io.PCP
    _dmemreq.io.VAddrE:=  _addr_cal.io.d_paddr //_alu.io.result//alu里面的结果计算出来可能就要用来取数据 // 数据地质一定是加法算出来的，不经过alu里面一堆高延迟路径
   _addr_cal.io.d_memrl := _id2ex.io2.MemRLE
   
              

    data_cache := commit_cache_reg &&   _addr_cal.io.d_cached.asBool
    data_size := Mux(mem_write,2.U,_dmemreq.io.size)

   // inst_cache := !disable_cache & _addr_cal.io.i_cached
    cp0_asid := _cp0.io.asid

    // _cp0.io.int_i := io.ext_int
    _cp0.io.int_i := int_instanceW
    _cp0.io.pc    := Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.PCW,PCW_Reg)//_mem22wb.io.PCW,方便调试罢了
    _cp0.io.mem_bad_vaddr := _mem22wb.io.BadVAddrW
    _cp0.io.cp0_write_en  := CP0WriteW.asBool || tlb_exception_co0_writeW
    _cp0.io.exception_type_i := ExceptionTypeW
    _cp0.io.in_delayslot     :=Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.InDelaySlotW,slot_Reg)
    _cp0.io.in_branchjump_jr   :=Mux(_mem22wb.io.PCW =/= 0.U,_mem22wb.io.BranchJump_JrW,branchjump_Jr_Reg)

    //tlb的这些东西全都不引入前递策略
    cp0_tlb_read_data := _cp0.io.cp0_tlb_read_data
    _cp0.io.cp0_tlb_write_data := tlb_read_dataM2.tlb_read_data
    _cp0.io.cp0_tlb_write_en    := _mem2mem2.io.Tlb_ControlM(1)
    _cp0.io.cp0_index_tlb_write_able := _mem22wb.io.Tlb_ControlW(2)


///----------------------------------------------------------------                                         
    _cfu.io.InException := _cp0.io.exception








//===               ==  =       ==  =   =   =   =   =   =   =   ==  ==  
    // _cfu.io.InstUnalignedF := _pc2if.io.InstUnalignedF
    _cfu.io.BranchD      := _cu.io.BranchD
    _cfu.io.JumpD        := pre_decoder_jump
    _cfu.io.JRD         := pre_decoder_jr
    _cfu.io.CanBranchD  :=  Mux(((id_exception | ex_exception |mem_exception | mem2_exception | wb_exception) === 0.U),1.U,0.U)
        // Mux((ExceptionTypeD_Out | _id2ex.io.ExceptionTypeE_Out | 
        // _ex2mem.io.ExceptionTypeM_Out | _mem22wb.io.ExceptionTypeW_Out) =/= 0.U, 0.U,1.U)
    _cfu.io.BranchD_Flag := pre_decoder_branchD_flag
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

    _cfu.io.CP0WriteW           := _mem22wb.io.CP0WriteW
    _cfu.io.HiLoWriteW          := _mem22wb.io.HiLoWriteW
    _cfu.io.WriteRegW           := _mem22wb.io.WriteRegW
    _cfu.io.RegWriteW           := RegWriteW

    _cfu.io.RsD                 := RsD
    _cfu.io.RtD                 := RtD
    _cfu.io.RsE                 := _id2ex.io.RsE
    _cfu.io.RtE                 := _id2ex.io.RtE   
    _cfu.io.MemRLE              := _id2ex.io2.MemRLE

    _cfu.io.CP0WriteM2 := _mem2mem2.io.CP0WriteM
    _cfu.io.WriteCP0AddrM2 := _mem2mem2.io.WriteCP0AddrM
    _cfu.io.WriteCP0SelM2  := _mem2mem2.io.WriteCP0SelM
    _cfu.io.WriteRegM2  :=  _mem2mem2.io.WriteRegM
    _cfu.io.MemToRegM2  :=  _mem2mem2.io.MemToRegM
    _cfu.io.RegWriteM2  :=  _mem2mem2.io.RegWriteM
    _cfu.io.HiLoWriteM2 := _mem2mem2.io.HiLoWriteM

    
    // _cfu.io.data_cache_stage2_stall := data_stage2_stall

    }
}

// object myCPU_core_test extends App{
//     (new ChiselStage).emitVerilog(new myCPU)
// }


