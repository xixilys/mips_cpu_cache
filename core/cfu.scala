package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class cfu extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle { 
        val     AddrPendingF = Input(UInt(1.W))
        val     DataPendingF = Input(UInt(1.W))
        val     Inst_Fifo_Empty = Input(UInt(1.W))

        
        val     BranchD = Input(UInt(6.W))
        val     JumpD = Input(UInt(1.W))
        val     JRD = Input(UInt(1.W))
        val     CanBranchD = Input(UInt(1.W))

        val     DivPendingE = Input(UInt(1.W))

        val     AddrPendingE = Input(UInt(1.W))
        val     DataPendingM = Input(UInt(1.W))
        
        val     InException = Input(UInt(1.W))
        val     MemRLE       = Input(UInt(2.W))

        val     WriteRegE = Input(UInt(5.W))
        val     MemToRegE = Input(UInt(1.W))
        val     RegWriteE = Input(UInt(1.W))
        val     HiLoToRegE = Input(UInt(2.W))
        val     CP0ToRegE = Input(UInt(1.W))
        
        val     WriteRegM = Input(UInt(5.W))
        val     MemToRegM = Input(UInt(1.W))
        val     RegWriteM = Input(UInt(1.W))
        val     HiLoWriteM = Input(UInt(2.W))
        val     CP0WriteM = Input(UInt(1.W))
        
        val     WriteRegW = Input(UInt(5.W))  
        val     RegWriteW = Input(UInt(1.W))
        val     HiLoWriteW = Input(UInt(2.W))
        val     CP0WriteW = Input(UInt(1.W))

        val    ReadCP0AddrE= Input(UInt(5.W))
        val    ReadCP0SelE= Input(UInt(3.W))

        val    WriteCP0AddrM= Input(UInt(5.W))
        val    WriteCP0SelM= Input(UInt(3.W))
        
        
        val     RsD = Input(UInt(5.W))
        val     RtD = Input(UInt(5.W))

        val     RsE = Input(UInt(5.W))
        val     RtE = Input(UInt(5.W))
        
        val    StallF = Output(UInt(1.W)) 
        val    StallD = Output(UInt(1.W))
        val    StallE = Output(UInt(1.W))
        val    StallM = Output(UInt(1.W))
        val    StallW = Output(UInt(1.W))
        val    FlushD = Output(UInt(1.W))//流水线冲刷
        val    FlushE = Output(UInt(1.W))
        val    FlushM = Output(UInt(1.W))
        val    FlushW = Output(UInt(1.W))

        val    ForwardAE = Output(UInt(2.W))
        val    ForwardBE = Output(UInt(2.W))
        val    ForwardAD = Output(UInt(1.W))
        val    ForwardBD = Output(UInt(1.W))
        
        
        val    ForwardHE = Output(UInt(2.W))
        val    ForwardCP0E = Output(UInt(1.W)) //只关注ex阶段读的前di
    })
    // val ForwardAE_Reg = RegInit(0.U(2.W))
    // val ForwardBE_Reg = RegInit(0.U(2.W))
    // val ForwardHE_Reg = RegInit(0.U(2.W))
    // io.ForwardAE := ForwardAE_Reg
    // io.ForwardBE := ForwardBE_Reg
    // io.ForwardHE := ForwardHE_Reg

    // val ForwardAD_Reg = RegInit(0.U(1.W))
    // val ForwardBD_Reg = RegInit(0.U(1.W))
    // io.ForwardAD := ForwardAD_Reg
    // io.ForwardBD := ForwardBD_Reg

    //wb阶段不需要加前递，因为regfile里面加了相关的算法
    io.ForwardAD := io.RsD =/= 0.U && io.RsD === io.WriteRegM && io.RegWriteM.asBool && !io.MemToRegM.asBool //当Rs为操作数的时候将该寄存器前递
    io.ForwardBD := io.RtD =/= 0.U && io.RtD === io.WriteRegM && io.RegWriteM.asBool && !io.MemToRegM.asBool //


    //exe阶段与
    

    io.ForwardAE := Mux(io.RsE =/= 0.U && io.RsE === io.WriteRegM && io.RegWriteM.asBool && !io.MemToRegM.asBool,"b10".U,Mux(
        io.RsE =/= 0.U && io.RsE === io.WriteRegW && io.RegWriteW.asBool && !io.MemToRegM.asBool,"b01".U,0.U)) //关闭所有与cache有关的前递

    io.ForwardBE := Mux(io.RtE =/= 0.U && io.RtE === io.WriteRegM && io.RegWriteM.asBool && !io.MemToRegM.asBool,"b10".U,Mux(
        io.RtE =/= 0.U && io.RtE === io.WriteRegW && io.RegWriteW.asBool && !io.MemToRegM.asBool,"b01".U,0.U))

    io.ForwardHE := Mux((io.HiLoToRegE & io.HiLoWriteM) =/= 0.U,"b10".U,Mux(
        (io.HiLoToRegE & io.HiLoWriteW) =/= 0.U,"b01".U,0.U ))

    io.ForwardCP0E := Mux(io.CP0ToRegE.asBool && io.CP0WriteM.asBool,Mux(
        Cat(io.ReadCP0AddrE,io.ReadCP0SelE(0)) === Cat(io.WriteCP0AddrM,io.WriteCP0SelM(0)) ,1.U,0.U ),0.U)
//感觉lm stall不是很有用，直接exe阶段前递就可以了来着
    //val lm_Stall = (io.RsD === io.RtE || io.RtD === io.RtE) && io.MemToRegE.asBool // Rs与Rt为操作数并且其中有一个是需要访存的时候读的，就得让流水线停止，等到数据读完（）
    
    val fifo_empty_stall = io.Inst_Fifo_Empty.asBool
    val br_Stall = (io.CanBranchD.asBool && (io.BranchD =/= 0.U) && 
        ((io.RegWriteE.asBool && (io.WriteRegE === io.RsD || io.WriteRegE === io.RtD)) || //直接要写的寄存器和冲突了
        (io.MemToRegM.asBool && (io.WriteRegM === io.RsD || io.WriteRegM === io.RtD)))) && !io.InException.asBool //需要从mem读的寄存器冲突了
    val jr_Stall = (io.JumpD.asBool && io.JRD.asBool) && 
        ((io.RegWriteE.asBool && (io.WriteRegE === io.RsD || io.WriteRegE === io.RtD)) || //直接要写的寄存器和冲突了
        (io.MemToRegM.asBool && (io.WriteRegM === io.RsD || io.WriteRegM === io.RtD)))  && !io.InException.asBool //需要从mem读的寄存器冲突了
    val divStall = io.DivPendingE.asBool//除法需要计算很多个时钟周期
    val cp0Stall = (io.CP0WriteM.asBool && io.CP0ToRegE.asBool ) || (io.CP0WriteW.asBool && io.CP0ToRegE.asBool )
    val ifStall = io.AddrPendingF.asBool 
    val dmemStall = io.DataPendingM.asBool
    val mem2regM_Stall = (io.RsE =/= 0.U && io.RsE === io.WriteRegM && io.RegWriteM.asBool && io.MemToRegM.asBool) || 
                (io.RtE =/= 0.U && io.RtE === io.WriteRegM && io.RegWriteM.asBool && io.MemToRegM.asBool) ||//mem阶段出现mem2reg并且此时需要前递时，停止流水线
                (io.RsD =/= 0.U && io.RsD === io.WriteRegM && io.RegWriteM.asBool && io.MemToRegM.asBool) || 
                (io.RtD =/= 0.U && io.RtD === io.WriteRegM && io.RegWriteM.asBool && io.MemToRegM.asBool) 


    val memrlStall = Mux(io.MemRLE === 0.U || io.MemToRegE.asBool,0.U.asBool,1.U.asBool)
    


    val has_Stall = /*lm_Stall ||*/br_Stall||jr_Stall||divStall.asBool||cp0Stall ||ifStall||dmemStall
    val excepStall = io.InException.asBool && has_Stall
    val excepFlush = io.InException.asBool 
 //Stall 摊位，池子
    io.StallF := Mux(reset.asBool,1.U,!(/*lm_Stall||*/br_Stall||jr_Stall||divStall||cp0Stall||dmemStall/*||excepStall*/||memrlStall||mem2regM_Stall || fifo_empty_stall))
    io.StallD := Mux(reset.asBool,1.U,!(/*lm_Stall||*/br_Stall||jr_Stall||divStall||cp0Stall||dmemStall/*||excepStall*/||memrlStall || mem2regM_Stall))
    io.StallE := Mux(reset.asBool,1.U,!(divStall||cp0Stall||dmemStall/*||excepStall*/ || mem2regM_Stall))
    io.StallM := Mux(reset.asBool,1.U,!(divStall||dmemStall/*||excepStall*/))
    io.StallW := Mux(reset.asBool,1.U,!(0.U/*excepStall*/))
//flush冲洗
    io.FlushD := Mux(reset.asBool,0.U, io.StallD.asBool &&( excepFlush /*|| fifo_empty_stall*/ ))
    io.FlushE := Mux(reset.asBool,0.U,((io.StallE.asBool && ( ifStall ||br_Stall|| jr_Stall ||memrlStall /*||| lm_Stall | br_Stall || jr_Stall */  )))|| excepFlush)
    io.FlushM := Mux(reset.asBool,0.U,((io.StallM.asBool && ( cp0Stall || divStall ||mem2regM_Stall.asBool)) || excepFlush))
    io.FlushW := Mux(reset.asBool,0.U,((io.StallW.asBool && ( dmemStall || excepFlush))|| divStall))

}

// object cfu_test extends App{
//     (new ChiselStage).emitVerilog(new cfu)
// }
