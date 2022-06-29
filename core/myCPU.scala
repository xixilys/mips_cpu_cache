package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import os.makeDir
/*
目前已经禁用了swl和lwl相关的功能功能
若想启用，修改  Mem_withRL_Data := _dmem.io.RD
修改mem_write



*/
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
    val   inst_rdata = IO(Input(UInt(32.W))).suggestName("inst_sram_rdata") 
    val   inst_hit = IO(Input(UInt(1.W))).suggestName("inst_sram_hit")
   
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
    val _if2id   = Module(new if2id)
    val _imem    = Module(new imem)
    val _mem2wb  = Module(new mem2wb)
    val _mmu     = Module(new mmu)
    val _muldiv  = Module(new muldiv)
    val _pc2if   = Module(new pc2if)
    val _regfile = Module(new regfile)

    // io.inst_port_test <> _pc2if.io.inst_port_test
    // inst_port_test <> _pc2if.io.inst_port_test
//    _if2id.io <> _pc2if.io
   _cu.io    <> _id2ex.io1
   _id2ex.io2 <> _ex2mem.io1

// -----------定义所有相关的变量-------
//-------------PC----------
    _pc2if.io.InstUnalignedP := _mmu.io.i_unaligned
    _pc2if.io.en             := _cfu.io.StallF
    _pc2if.io.ReturnPCW      := _cp0.io.return_pc
    _pc2if.io.inst_port_test.inst_hit := inst_hit
    _pc2if.io.inst_cache     := inst_cache
    inst_size := _pc2if.io.inst_port_test.inst_size


    _imem.io.inst_req :=  _pc2if.io.inst_port_test.inst_req
    _imem.io.inst_rdata :=  inst_rdata 
    _imem.io.inst_addr_ok := inst_addr_ok
    _imem.io.inst_data_ok :=inst_data_ok
    _imem.io.inst_hit := inst_hit
    _imem.io.InstUnalignedF := _pc2if.io.InstUnalignedF
    _imem.io.inst_cache   := inst_cache

    inst_addr := _pc2if.io.inst_port_test.inst_addr
    inst_req  :=  _pc2if.io.inst_port_test.inst_req
    inst_wr   := 0.U//_pc2if.io.inst_port_test.inst_wr
    inst_wdata:= _pc2if.io.inst_port_test.inst_wdata

    _pc2if.io.inst_port_test.inst_rdata := inst_rdata
    _pc2if.io.inst_port_test.inst_addr_ok := inst_addr_ok
    _pc2if.io.inst_port_test.inst_data_ok := inst_data_ok

//-------------IF----------

    _if2id.io.ReadDataF     := _imem.io.ReadDataF
    _if2id.io.en            :=  _cfu.io.StallD
    _if2id.io.clr           := _cfu.io.FlushD
    val PCPlus8F = _pc2if.io.PCF + 8.U
    val PCPlus4F = _pc2if.io.PCF + 4.U
    val ExceptionTypeF = Mux(_pc2if.io.InstUnalignedF.asBool,EXCEP_MASK_AdELI,0.U)//3）取指PC不对齐于字边界，指令不对齐例外,由mmu处获得
    val NextDelaySlotD = (_cu.io.BranchD_Flag.asBool || _cu.io.JumpD.asBool) && !_pc2if.io.InstUnalignedF.asBool //此时指令对齐才有可能为延迟曹
    //-------------ID----------
    _id2ex.io.en            :=  _cfu.io.StallE
    _id2ex.io.clr           := _cfu.io.FlushE
    val InstrD = _if2id.io.InstrD
    val BranchRsD = Wire(UInt(32.W))//Mux(_cfu.io.ForwardAD.asBool,ResultM,_regfile.io.RD1)//前递，降低流水线阻塞,后面是指需要读寄存器的时候的值
    val BranchRtD = Wire(UInt(32.W))
    val PCPLus4D = _if2id.io.PCD + 4.U
    val PCPLus8D = _if2id.io.PCD + 8.U 
    val PCSrcD  = Cat(_cu.io.JumpD,_br.io.exe)//Wire(UInt(2.W)) 要不要跳转 + 有没有分支指令需要跳转
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
    _ex2mem.io.clr           := _cfu.io.FlushM
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

   
// ---------- PC ----------
    //_pc2if.io <> io//sram like增加AXI总线接口，仅仅增加了一个握手

    
    val Pc_Next = Mux2_4(PCSrcD,PCPlus4F,PCBranchD,PCJumpD ,0.U)

    _pc2if.io.PC_next           := Pc_Next
    _pc2if.io.InstUnalignedP    := _mmu.io.i_unaligned
    _pc2if.io.PhyAddrP          := _mmu.io.i_paddr
    _pc2if.io.ExceptionW        := _cp0.io.exception


//-------------IF----------
    val PCF =     _if2id.io.PCF 
    // _pc2if.io <> _imem.io
    _if2id.io.en      := _cfu.io.StallD //相当于清空ID阶段的所有数据
    _if2id.io.clr     := _cfu.io.FlushD
    _if2id.io.PCPlus4F       := PCPlus4F
    _if2id.io.PCPlus8F       := PCPlus8F
    _if2id.io.ReadDataF      := Mux(_cu.io1.BadInstrD.asBool || _cu.io1.SysCallD.asBool||_cu.io1.BreakD.asBool||_cu.io1.EretD.asBool,0.U,_imem.io.ReadDataF)//如果延迟曹指令要跳转的话，就直接不运行这一条指令，避免对后面的操作产生影响
    _if2id.io.PCF            := _pc2if.io.PCF
    _if2id.io.ExceptionTypeF := ExceptionTypeF
    _if2id.io.NextDelaySlotD  := NextDelaySlotD   //下一条为延迟槽，ID阶段解码出来
    // _if2id.io.NextDelaySlotD :=   
    
//-------------ID----------
    val BadVAddrD = Mux(Pc_Next(1,0) =/= 0.U,Pc_Next,0.U) 
    _regfile.io.A1 := InstrD(25,21)
    _regfile.io.A2 := InstrD(20,16) 
    _cu.io1.InstrD := _if2id.io.InstrD
    _br.io.rs := BranchRsD
    BranchRsD:= Mux(_cfu.io.ForwardAD.asBool,Forward_ResultM,_regfile.io.RD1)//前递，降低流水线阻塞,后面是指需要读寄存器的时候的值
    _br.io.rt := BranchRtD
    BranchRtD:= Mux(_cfu.io.ForwardBD.asBool,Forward_ResultM,_regfile.io.RD2)
    _br.io.en := Mux(((_if2id.io.ExceptionTypeD_Out | _id2ex.io.ExceptionTypeE_Out |//有例外的话
    _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out) === 0.U),1.U,0.U) //不允许触发分支
    _br.io.branch := _cu.io.BranchD
  
    
    _id2ex.io.ExceptionTypeD  := Mux(_if2id.io.ExceptionTypeD_Out === 0.U,(
        (Mux(_cu.io1.BadInstrD.asBool,EXCEP_MASK_RI,0.U)) | 
        (Mux(_cu.io1.SysCallD.asBool,EXCEP_MASK_Sys,0.U)) |
        (Mux(_cu.io1.BreakD.asBool,EXCEP_MASK_Bp,0.U))    |
        (Mux(_cu.io1.EretD.asBool,EXCEP_MASK_ERET,0.U)) ),_if2id.io.ExceptionTypeD_Out)

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
    _id2ex.io.InDelaySlotD := _if2id.io.InDelaySlotD
    _id2ex.io.PCD          := _if2id.io.PCD
    _id2ex.io.BranchJump_JrD  := Cat(0.U(1.W),_cu.io.JRD.asBool||_cu.io.BranchD_Flag.asBool || _cu.io.JumpD.asBool)
    _id2ex.io.BadVaddrD    := BadVAddrD


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



//-------------WB----------
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
    disable_cache := Mux((PCF === 0xbfc0005.U<<4 && _imem.io.ReadDataF === Cat(0x3c1d.U,0xbfc1.U)),1.U,disable_cache)

    _mmu.io.i_en := _cfu.io.StallF
    _mmu.io.d_en := Mux((_id2ex.io.ExceptionTypeE_Out | _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out)
        =/= 0.U,0.U,_cfu.io.StallE & (_id2ex.io2.MemToRegE | _id2ex.io2.MemWriteE))
    _mmu.io.d_clr := 0.U  
    _mmu.io.d_width := _id2ex.io2.MemWidthE  
    _mmu.io.i_vaddr := _pc2if.io.PCP
    _mmu.io.d_vaddr :=  Src1E + Src2E//_alu.io.result//alu里面的结果计算出来可能就要用来取数据 // 数据地质一定是加法算出来的，不经过alu里面一堆高延迟路径
    _mmu.io.d_memrl := _id2ex.io2.MemRLE | _ex2mem.io.MemRLM
    
              

    data_cache := !disable_cache & Mux(mem_write,mem_cached, _mmu.io.d_cached ) //
    data_size := Mux(mem_write,2.U,_dmemreq.io.size)

    inst_cache := !disable_cache & _mmu.io.i_cached
  

    // _cp0.io.int_i := io.ext_int
    _cp0.io.int_i := 0.U
    _cp0.io.pc    := Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.PCW,PCW_Reg)//_mem2wb.io.PCW,方便调试罢了
    _cp0.io.mem_bad_vaddr := _mem2wb.io.BadVAddrW
    _cp0.io.cp0_write_en  := CP0WriteW 
    _cp0.io.exception_type_i := ExceptionTypeW
    _cp0.io.in_delayslot     :=Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.InDelaySlotW,slot_Reg)
    _cp0.io.in_branchjump_jr   :=Mux(_mem2wb.io.PCW =/= 0.U,_mem2wb.io.BranchJump_JrW,branchjump_Jr_Reg)

    _cfu.io.InExceptionF := _pc2if.io.InExceptionF
    // _cfu.io.InstUnalignedF := _pc2if.io.InstUnalignedF
    _cfu.io.BranchD      := _cu.io.BranchD
    _cfu.io.JumpD        := _cu.io.JumpD
    _cfu.io.JRD         := _cu.io.JRD
    _cfu.io.CanBranchD  := Mux((_if2id.io.ExceptionTypeD_Out | _id2ex.io.ExceptionTypeE_Out | 
        _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out) =/= 0.U, 0.U,1.U)
    _cfu.io.DivPendingE         := _muldiv.io.pending
    _cfu.io.AddrPendingE        := _dmemreq.io.addr_pending //data地址等待
    _cfu.io.DataPendingM        := _dmem.io.data_pending // data数据等待
    _cfu.io.AddrPendingF        := _pc2if.io.addr_pending // 指令地址等待
    _cfu.io.DataPendingF        := _pc2if.io.data_pending //指令数据等待

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

// object myCPU_test extends App{
//     (new ChiselStage).emitVerilog(new myCPU)
// }
