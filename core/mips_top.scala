package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
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
class mips_top extends Module with mips_macros {//
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
        val   ext_int = Input(UInt(6.W))

        // val   inst_req = Output(UInt(1.W))
        // val   inst_wr = Output(UInt(1.W))
        // val   inst_size = Output(UInt(2.W))
        // val   inst_addr = Output(UInt(32.W))
        // val   inst_wdata = Output(UInt(32.W))
        val   inst_cache = Output(UInt(1.W))
        // val   inst_addr_ok = Input(UInt(1.W))
        // val   inst_data_ok = Input(UInt(1.W))
        // val   inst_rdata = Input(UInt(32.W))
        val inst_port_test       =    new inst_port

        val   data_req = Output(UInt(1.W))
        val   data_wr = Output(UInt(1.W))
        val   data_size = Output(UInt(2.W))
        val   data_addr = Output(UInt(32.W))
        val   data_wdata = Output(UInt(32.W))
        val   data_cache = Output(UInt(1.W))
        val   data_addr_ok = Input(UInt(1.W))
        val   data_data_ok = Input(UInt(1.W))
        val   data_rdata = Input(UInt(32.W))
    })

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

    io.inst_port_test <> _pc2if.io.inst_port_test
//    _if2id.io <> _pc2if.io
   _cu.io    <> _id2ex.io1
   _id2ex.io2 <> _ex2mem.io1

// -----------定义所有相关的变量-------
//-------------PC----------
    _pc2if.io.InstUnalignedP := _mmu.io.i_unaligned
    _pc2if.io.en             := _cfu.io.StallF
    _pc2if.io.ReturnPCW      := _cp0.io.return_pc


    _imem.io.inst_req :=  _pc2if.io.inst_port_test.inst_req
    _imem.io.inst_rdata :=  io.inst_port_test.inst_rdata 
    _imem.io.inst_addr_ok := io.inst_port_test.inst_addr_ok
    _imem.io.inst_data_ok := io.inst_port_test.inst_data_ok
    _imem.io.InstUnalignedF := _pc2if.io.InstUnalignedF
//-------------IF----------
    // val 
    _if2id.io.ReadDataF     := _imem.io.ReadDataF
    _if2id.io.en            :=  _cfu.io.StallD
    _if2id.io.clr           := _cfu.io.FlushD
    val PCPlus8F = _pc2if.io.PCF + 8.U
    val PCPlus4F = _pc2if.io.PCF + 4.U
    val ExceptionTypeF = Mux(_pc2if.io.InstUnalignedF.asBool,EXCEP_MASK_AdELI,0.U)//3）取指PC不对齐于字边界，指令不对齐例外,由mmu处获得
    val NextDelaySlotD = _cu.io.BranchD_Flag.asBool || _cu.io.JumpD.asBool
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
    val PCJumpD   = Cat(PCPLus4D(31,28),InstrD(25,0),0.U(2.W))  //Mux(_cu.io.JRD.asBool,BranchRsD,PCPLus4D)// 分支指令均需要读寄存器并且进行比较,由于我是在解码阶段即判断到底需不需要跳转，所以需要用id 阶段的pc进行运算
    val RsD       = InstrD(25,21)
    val RtD       = InstrD(20,16)
    val RdD       = InstrD(15,11)
    val ImmD      = Mux(_cu.io.ImmUnsigned.asBool, unsign_extend(InstrD(15,0),16),sign_extend(InstrD(15,0),16))
    val Write_WriteCP0AddrW = InstrD(15,11)
    val Write_WriteCP0Sel0  = InstrD(2,0)

//-------------EX----------
    _ex2mem.io.en            :=  _cfu.io.StallE
    _ex2mem.io.clr           := _cfu.io.FlushE
    val RD1ForWardE = Wire(UInt(32.W))
    val RD2ForWardE = Wire(UInt(32.W))
    val WriteDataE = RD2ForWardE
    val BadVAddrE  = Wire(UInt(32.W))

    io.data_req := _dmemreq.io.req
    io.data_wr  := _dmemreq.io.wr
    io.data_size := _dmemreq.io.size
    io.data_addr := _dmemreq.io.addr
    io.data_wdata := _dmemreq.io.wdata
  
//-------------MEM----------
    _mem2wb.io.en            :=  _cfu.io.StallM
    _mem2wb.io.clr           := _cfu.io.FlushM
    _dmem.io.req        := _dmemreq.io.req
    _dmem.io.addr_ok    := io.data_addr_ok
    _dmem.io.data_ok    := io.data_data_ok
    _dmem.io.rdata      := io.data_rdata
    _dmem.io.ReadEn         := _ex2mem.io.MemToRegM
    _dmem.io.WIDTH      := _ex2mem.io.MemWidthM
    _dmem.io.SIGN := !_ex2mem.io.LoadUnsignedM.asBool
    // _dmem.io.WE   :=  _ex2mem.io.MemWriteM
    // _dmem.io.WIDTH := _ex2mem.io.MemWidthM
    // _dmem.io.Physisc_Address := _ex2mem.io.PhyAddrM
    // _dmem.io.SIGN       := _ex2mem.io.LoadUnsignedM
    _dmem.io.Physisc_Address := _ex2mem.io.PhyAddrM

    // _dmem.io <> _dmemreq.io

    // _dmemreq.io <>  io // dmem就是控制读数据的来着
    val ResultM = Wire(UInt(32.W))
    val HiInM   = _ex2mem.io.HiInM
    val LoInM   = _ex2mem.io.LoInM
    val PCPlus8M = _ex2mem.io.PCM + 8.U
    _mem2wb.io.RegWriteM := _ex2mem.io.RegWriteM
    _mem2wb.io.MemToRegM  :=  _ex2mem.io.MemToRegM
    
    _mem2wb.io.MemToRegM            := _ex2mem.io.MemToRegM
    _mem2wb.io.WriteRegM            := _ex2mem.io.WriteRegM
    _mem2wb.io.HiInM                := _ex2mem.io.HiInM
    _mem2wb.io.LoInM                := _ex2mem.io.LoInM
    _mem2wb.io.InDelaySlotM         := _ex2mem.io.InDelaySlotM
    _mem2wb.io.BadVAddrM            := _ex2mem.io.BadVAddrM
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

   
// ---------- PC ----------
    //_pc2if.io <> io//sram like增加AXI总线接口，仅仅增加了一个握手


    val Pc_Next = Mux2_4(PCSrcD,PCPlus4F,PCBranchD,BranchRsD,0.U)

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
    _if2id.io.ReadDataF      := _imem.io.ReadDataF
    _if2id.io.PCF            := _pc2if.io.PCF
    _if2id.io.ExceptionTypeF := ExceptionTypeF
    _if2id.io.NextDelaySlotD  := NextDelaySlotD   //下一条为延迟槽，ID阶段解码出来
    // _if2id.io.NextDelaySlotD :=   
    
//-------------ID----------
    _regfile.io.A1 := InstrD(25,21)
    _regfile.io.A2 := InstrD(20,16) 
    _cu.io1.InstrD := _if2id.io.InstrD
    _br.io.rs := BranchRsD
    BranchRsD:= Mux(_cu.io.JRD.asBool,Mux(_cfu.io.ForwardAD.asBool,ResultM,_regfile.io.RD1),PCJumpD)//前递，降低流水线阻塞,后面是指需要读寄存器的时候的值
    _br.io.rt := BranchRtD
    BranchRtD:= Mux(_cfu.io.ForwardBD.asBool,ResultM,_regfile.io.RD2)
    _br.io.en := Mux(((_if2id.io.ExceptionTypeD_Out | _id2ex.io.ExceptionTypeE_Out |//有例外的话
        _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out) === 0.U),1.U,0.U) //不允许触发分支
    _br.io.branch := _cu.io.BranchD
    
    _id2ex.io.ExceptionTypeD  := Mux(_if2id.io.ExceptionTypeD_Out === 0.U,(
        (Mux(_cu.io1.BadInstrD.asBool,EXCEP_MASK_RI,0.U)) | 
        (Mux(_cu.io1.SysCallD.asBool,EXCEP_MASK_Sys,0.U)) |
        (Mux(_cu.io1.BreakD.asBool,EXCEP_MASK_Bp,0.U))    |
        (Mux(_cu.io1.EretD.asBool,EXCEP_MASK_ERET,0.U))),_if2id.io.ExceptionTypeD_Out)

    _id2ex.io.RsD := RsD
    _id2ex.io.RtD := RtD 
    _id2ex.io.RdD := RdD 
    _id2ex.io.ImmD:= ImmD
    _id2ex.io.RD1D:= Mux(_cfu.io.ForwardAD.asBool,ResultM,_regfile.io.RD1)
    _id2ex.io.RD2D:= Mux(_cfu.io.ForwardBD.asBool,ResultM,_regfile.io.RD2)
    _id2ex.io.WriteCP0AddrD := InstrD(15,11)
    _id2ex.io.WriteCP0SelD  := InstrD(2,0)
    _id2ex.io.ReadCP0AddrD  := InstrD(15,11)
    _id2ex.io.ReadCP0SelD  := InstrD(2,0)
    _id2ex.io.PCPlus8D      := PCPLus8D
    _id2ex.io.InDelaySlotD := _if2id.io.InDelaySlotD
    _id2ex.io.PCD          := _if2id.io.PCD

//-------------EX----------
    //前递还没写
    val RD1ForWardE_p   = Mux2_4(_cfu.io.ForwardAE,_id2ex.io.RD1E,ResultW,ResultM,0.U)
    val RD2ForWardE_p   = Mux2_4(_cfu.io.ForwardBE,_id2ex.io.RD2E,ResultW,ResultM,0.U) 
    val RD1ForWardE_r   = RegInit(0.U(32.W))  
    val RD2ForWardE_r   = RegInit(0.U(32.W))
    val Forward_Lock1E  = RegInit(0.U(1.W)) 
    val Forward_Lock2E  = RegInit(0.U(1.W))
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

    val HiInE = Mux(_id2ex.io2.HiLoWriteE === "b10".U,WriteCP0HiLoDataE,_muldiv.io.hi) // b10 对应MTHI指令，通用寄存器到HI寄存器
    val LoInE = Mux(_id2ex.io2.HiLoWriteE === "b01".U,WriteCP0HiLoDataE,_muldiv.io.lo) // b10 对应MTHI指令，通用寄存器到HI寄存器

    BadVAddrE:= Mux(_mmu.io.d_unaligned.asBool,_alu.io.result,_id2ex.io2.PCE) //mmu模块发现数据指令错位,下面还需要加上数据错误例外
    
    _dmemreq.io.en := Mux((_id2ex.io.ExceptionTypeE_Out | _ex2mem.io.ExceptionTypeM_Out | 
        _mem2wb.io.ExceptionTypeW_Out) =/= 0.U,0.U,(!_mmu.io.d_unaligned.asBool && _cfu.io.StallE.asBool
        && (_id2ex.io2.MemWriteE.asBool || _id2ex.io2.MemToRegE.asBool))) // 后面几级流水线没有出现例外，加上流水线没有停止，
        // 再加上有mem到reg的操作或者写Mem的操作，才会有所谓的mem access，其实就是使能memreq

    _dmemreq.io.MemToRegE := _id2ex.io2.MemToRegE
    _dmemreq.io.MemWidthE := _id2ex.io2.MemWidthE
    _dmemreq.io.PhyAddrE  := _mmu.io.d_paddr
    _dmemreq.io.WriteDataE := WriteDataE
    _dmemreq.io.MemWriteE := _id2ex.io2.MemWriteE
    _dmemreq.io.addr_ok   := io.data_addr_ok

    //读寄存器HI 和 LO,还没有完全看懂，包括了前递对寄存器的影响
    _ex2mem.io.HiLoOutE := Mux1H(Seq(
        _id2ex.io2.HiLoToRegE(0) -> Mux2_4(_cfu.io.ForwardHE,_hilo.io.lo_o,LoInW,LoInM ,0.U),
        _id2ex.io2.HiLoToRegE(1) -> Mux2_4(_cfu.io.ForwardHE,_hilo.io.hi_o,HiInW,HiInM ,0.U)
    ))
    _ex2mem.io.ExceptionTypeE := Mux(_id2ex.io.ExceptionTypeE_Out =/= 0.U,_id2ex.io.ExceptionTypeE_Out,
        (Mux(_mmu.io.d_unaligned.asBool && _id2ex.io2.MemToRegE.asBool ,EXCEP_MASK_AdELD,0.U)) |
        (Mux(_mmu.io.d_unaligned.asBool && _id2ex.io2.MemWriteE.asBool ,EXCEP_MASK_AdES,0.U))  |
        (Mux(_alu.io.overflow.asBool,EXCEP_MASK_Ov,0.U))) //取或运算便于搞事,之后要多减少位数，感觉32位稍微有点长了
    _ex2mem.io.ReadCP0DataE := _cp0.io.cp0_read_data//cp0的读取数据
    _ex2mem.io.CP0WriteE    := _id2ex.io2.CP0WriteE
    _ex2mem.io.ALUOutE := _alu.io.result
    _ex2mem.io.BadVAddrE := BadVAddrE
    _ex2mem.io.HiInE    := HiInE
    _ex2mem.io.LoInE    := LoInE
    _ex2mem.io.WriteRegE := WriteRegE//要写的寄存器
    _ex2mem.io.WriteDataE := WriteDataE
    _ex2mem.io.WriteCP0HiLoDataE := WriteCP0HiLoDataE
    _ex2mem.io.PhyAddrE   := _mmu.io.d_paddr//有一点疑问，mmu输出有触发器，怎么保证可以计算正常的


    // _ex2mem.io1.HiLoWriteE := _id2ex.io2.HiLoWriteE

    _cp0.io.cp0_read_addr := _id2ex.io2.ReadCP0AddrE
    _cp0.io.cp0_read_sel := _id2ex.io2.ReadCP0SelE
    _cp0.io.cp0_write_addr := _id2ex.io2.WriteCP0AddrE
    _cp0.io.cp0_write_sel := _id2ex.io2.WriteCP0SelE
    _cp0.io.cp0_write_data := _mem2wb.io.WriteCP0HiLoDataW

    _hilo.io.hi_i := _mem2wb.io.HiInW
    _hilo.io.lo_i := _mem2wb.io.LoInW//写回阶段，将寄存器的值写回
    




//-------------MEM----------
    // val Data_pending_state = _dmem.io.data_pending
    ResultM := MuxCase(_ex2mem.io.ALUOutM,Seq(
        _ex2mem.io.LinkM.asBool -> PCPlus8M,
        _ex2mem.io.CP0ToRegM.asBool -> _ex2mem.io.ReadCP0DataM,
       ( _ex2mem.io.HiLoToRegM =/= 0.U) -> _ex2mem.io.HiLoOutM
    ))

 
    // --------------
    _mem2wb.io.ReadDataM := _dmem.io.RD
    _mem2wb.io.ExceptionTypeM := _ex2mem.io.ExceptionTypeM_Out

    _mem2wb.io.ResultM        := ResultM
//-------------WB----------
    ResultW   := Mux(_mem2wb.io.MemToRegW.asBool,_mem2wb.io.ReadDataW,_mem2wb.io.ResultW)
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
    _mmu.io.d_vaddr := _alu.io.result//alu里面的结果计算出来可能就要用来取数据
    
              

    io.inst_cache := !disable_cache & _mmu.io.i_cached
    io.data_cache := !disable_cache & _mmu.io.d_cached

    _cp0.io.int_i := io.ext_int
    _cp0.io.pc    := _mem2wb.io.PCW
    _cp0.io.mem_bad_vaddr := BadVAddrE
    _cp0.io.cp0_write_en  := CP0WriteW 
    _cp0.io.exception_type_i := ExceptionTypeW
    _cp0.io.in_delayslot     := _mem2wb.io.InDelaySlotW

    _cfu.io.InExceptionF := _pc2if.io.InExceptionF
    _cfu.io.BranchD      := _cu.io.BranchD
    _cfu.io.JumpD        := _cu.io.JumpD
    _cfu.io.JRD         := _cu.io.JRD
    _cfu.io.CanBranchD  := Mux((_if2id.io.ExceptionTypeD_Out | _id2ex.io.ExceptionTypeE_Out | 
        _ex2mem.io.ExceptionTypeM_Out | _mem2wb.io.ExceptionTypeW_Out) =/= 0.U, 0.U,1.U)
    _cfu.io.DivPendingE         := _muldiv.io.pending
    _cfu.io.AddrPendingE        := _dmemreq.io.addr_pending //data地址等待
    // _cfu.io.DataPendingM        := _dmem.io.data_pending // data数据等待
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
//为啥在写回阶段仍然要写寄存器捏
    _cfu.io.CP0WriteW           := _mem2wb.io.CP0WriteW
    _cfu.io.HiLoWriteW          := _mem2wb.io.HiLoWriteW
    _cfu.io.WriteRegW           := _mem2wb.io.WriteRegW
    _cfu.io.RegWriteW           := _mem2wb.io.WriteRegW

    _cfu.io.RsD                 := RsD
    _cfu.io.RtD                 := RtD
    _cfu.io.RsE                 := _id2ex.io.RsE
    _cfu.io.RtE                 := _id2ex.io.RtE    

}

// object mips_top_test extends App{
//     (new ChiselStage).emitVerilog(new mips_top)
// }
