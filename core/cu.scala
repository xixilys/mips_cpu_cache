package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
class decoder_port extends Bundle {
        val   BranchD_Flag  =   Output(UInt(1.W))
        val   RegWriteD     =   Output(UInt(1.W))
        val   MemToRegD     =   Output(UInt(1.W))
        val   MemWriteD     =   Output(UInt(1.W))
        val   ALUCtrlD      =   Output(UInt(24.W)) // 独热码
        val   ALUSrcD       =   Output(UInt(2.W))
        val   RegDstD=      Output(UInt(2.W))
        val   ImmUnsigned=      Output(UInt(1.W))
        val   BranchD=      Output(UInt(6.W))
        val   JumpD=      Output(UInt(1.W))
        val   JRD=      Output(UInt(1.W))
        val   LinkD=      Output(UInt(1.W))
        val   HiLoWriteD=      Output(UInt(2.W))
        val   HiLoToRegD=      Output(UInt(2.W))
        val   CP0WriteD=      Output(UInt(1.W))
        val   CP0ToRegD=      Output(UInt(1.W))
        val   LoadUnsignedD=      Output(UInt(1.W))
        val   MemWidthD=      Output(UInt(2.W))
        val   MemRLD=      Output(UInt(2.W))
        // val   BadInstrD=      Output(UInt(1.W))
        // val   BreakD=      Output(UInt(1.W))
        // val   SysCallD=      Output(UInt(1.W))
        // val   EretD   =   Output(UInt(1.W))
}
class cu_1 extends Module with mips_macros {
    val io1 = IO(new Bundle{
        val   InstrD = Input(UInt(32.W))
        val   BadInstrD=      Output(UInt(1.W))
        val   BreakD=      Output(UInt(1.W))
        val   SysCallD=      Output(UInt(1.W))
        val   EretD   =   Output(UInt(1.W))
    })   
    val io = IO(new decoder_port 
 //问题肯定会出在这
        // val   InstrD  =   Input(UInt(32.W))

        // val   BranchD_Flag  =   Output(UInt(1.W))
        // val   RegWriteD     =   Output(UInt(1.W))
        // val   MemToRegD     =   Output(UInt(1.W))
        // val   MemWriteD     =   Output(UInt(1.W))
        // val   ALUCtrlD      =   Output(UInt(24.W)) // 独热码
        // val   ALUSrcD       =   Output(UInt(2.W))
        // val   RegDstD=      Output(UInt(2.W))
        // val   ImmUnsigned=      Output(UInt(1.W))
        // val   BranchD=      Output(UInt(6.W))
        // val   JumpD=      Output(UInt(1.W))
        // val   JRD=      Output(UInt(1.W))
        // val   LinkD=      Output(UInt(1.W))
        // val   HiLoWriteD=      Output(UInt(2.W))
        // val   HiLoToRegD=      Output(UInt(2.W))
        // val   CP0WriteD=      Output(UInt(1.W))
        // val   CP0ToRegD=      Output(UInt(1.W))
        // val   LoadUnsignedD=      Output(UInt(1.W))
        // val   MemWidthD=      Output(UInt(2.W))
        // val   BadInstrD=      Output(UInt(1.W))
        // val   BreakD=      Output(UInt(1.W))
        // val   SysCallD=      Output(UInt(1.W))
        // val   EretD   =   Output(UInt(1.W))

    )

    
    val OpD = io1.InstrD(31,26)//首字母为o的大写(>_<)
    val FunctD = io1.InstrD(5,0)
    val RsD    = io1.InstrD(25,21)
    val RtD    = io1.InstrD(20,16)
    val ins_id = Wire(UInt(64.W))
    // io.BranchD_Flag := Mux(OpD === )
    ins_id := MuxLookup(OpD,1.U<<ID_NULL,Seq(
        ( OP_ADDI) -> (1.U<<ID_ADDI),
        ( OP_ANDI) -> (1.U<<ID_ANDI),
        ( OP_ADDIU) -> (1.U<<ID_ADDIU),
        ( OP_SLTI) -> (1.U<<ID_SLTI),
        ( OP_SLTIU) -> (1.U<<ID_SLTIU),
        ( OP_LUI) -> (1.U<<ID_LUI),
        ( OP_ORI) -> (1.U<<ID_ORI),
        ( OP_XORI) -> (1.U<<ID_XORI),
        ( OP_BEQ) -> (1.U<<ID_BEQ),
        ( OP_BNE ) -> (1.U<<ID_BNE ),
        ( OP_BGTZ) -> (1.U<<ID_BGTZ),
        ( OP_BLEZ) -> (1.U<<ID_BLEZ),
        ( OP_J   ) -> (1.U<<ID_J ),
        ( OP_JAL) -> (1.U<<ID_JAL),
        ( OP_LB) -> (1.U<<ID_LB),
        ( OP_LBU) -> (1.U<<ID_LBU),
        ( OP_LH) -> (1.U<<ID_LH),
        ( OP_LHU ) -> (1.U<<ID_LHU ),
        ( OP_LW ) ->(1.U<<ID_LW),
        ( OP_SB) -> (1.U<<ID_SB),
        ( OP_SH) -> (1.U<<ID_SH),
        ( OP_SW) -> (1.U<<ID_SW),
        (OP_LWL ) -> (1.U<<ID_LWL),
        (OP_LWR ) -> (1.U<<ID_LWR),
        (OP_SWL ) -> (1.U<<ID_SWL),
        (OP_SWR ) -> (1.U<<ID_SWR),
        ( OP_SPECIAL) -> MuxLookup(FunctD,1.U<<ID_NULL,Seq( // 在op相同情况下，根据funct来判断是哪一条指令 这个得写特判
            ( FUNC_SUB) ->   (1.U<<ID_SUB),
            ( FUNC_AND ) ->   (1.U<<ID_AND ),
            ( FUNC_OR) ->   (1.U<<ID_OR),
            ( FUNC_SLT) ->   (1.U<<ID_SLT),
            ( FUNC_SLL) ->   (1.U<<ID_SLL),
            ( FUNC_SLTU ) ->   (1.U<<ID_SLTU ),
            ( FUNC_XOR) ->   (1.U<<ID_XOR),
            ( FUNC_ADD) ->   (1.U<<ID_ADD),
            ( FUNC_ADDU ) ->   (1.U<<ID_ADDU ),
            ( FUNC_SUBU ) ->   (1.U<<ID_SUBU ),
            ( FUNC_DIV) ->   (1.U<<ID_DIV),
            ( FUNC_DIVU) ->   (1.U<<ID_DIVU),
            ( FUNC_MULT) ->   (1.U<<ID_MULT),
            ( FUNC_MULTU) ->   (1.U<<ID_MULTU),
            ( FUNC_NOR) ->   (1.U<<ID_NOR),
            ( FUNC_SLLV ) ->   (1.U<<ID_SLLV ),
            ( FUNC_SRA) ->   (1.U<<ID_SRA),
            ( FUNC_SRAV) ->   (1.U<<ID_SRAV),
            ( FUNC_SRL) ->   (1.U<<ID_SRL),
            ( FUNC_SRLV) ->   (1.U<<ID_SRLV ),
            ( FUNC_JR) ->   (1.U<<ID_JR),
            ( FUNC_JALR) ->   (1.U<<ID_JALR),
            ( FUNC_MFHI ) ->   (1.U<<ID_MFHI ),
            ( FUNC_MFLO) ->   (1.U<<ID_MFLO ),
            ( FUNC_MTHI) ->   (1.U<<ID_MTHI),
            ( FUNC_MTLO) ->   (1.U<<ID_MTLO),
            ( FUNC_BREAK ) ->   (1.U<<ID_BREAK ),
            ( FUNC_SYSCALL ) ->   (1.U<<ID_SYSCALL )
            )),
        ( OP_BRANCH) -> MuxCase(1.U<<ID_NULL,Array( //后面这里可以改,在id时就开始算分支
            (RtD === RT_BGEZ )  -> (1.U<<ID_BGEZ ),
            (RtD === RT_BGEZAL )  -> (1.U<<ID_BGEZAL ),
            (RtD === RT_BLTZ )  -> (1.U<<ID_BLTZ ),
            (RtD === RT_BLTZAL )  -> (1.U<<ID_BLTZAL )
        )),
        ( OP_PRIVILEGE) -> MuxCase(1.U<<ID_NULL,Array( //后面这里可以改,在id时就开始算分支
            (RsD === RS_ERET )  -> (1.U<<ID_ERET ),
            (RsD === RS_MFC0 )  -> (1.U<<ID_MFC0 ),
            (RsD === RS_MTC0 )  -> (1.U<<ID_MTC0 )
        ))

    ))

    io1.BadInstrD := ins_id === (1.U<<ID_NULL )
    io1.BreakD    := ins_id === (1.U<<ID_BREAK)
    io1.SysCallD  := ins_id === (1.U<<ID_SYSCALL)
    io1.EretD     := ins_id === (1.U<<ID_ERET)

    val get_controls = Wire(UInt(29.W))
    io.MemRLD       := get_controls(28,27)
    io.BranchD_Flag := get_controls(26)
    io.RegWriteD := get_controls(25)   //实在不知道咋写呜呜呜
    io.RegDstD   := get_controls(24,23) 
    io.ALUSrcD   := get_controls(22,21)
    io.ImmUnsigned := get_controls(20) 
    io.BranchD   := get_controls(19,14)
    io.JumpD     := get_controls(13)
    io.JRD       := get_controls(12) 
    io.LinkD     := get_controls(11)
    io.HiLoWriteD:= get_controls(10,9)
    io.HiLoToRegD := get_controls(8,7)
    io.CP0WriteD  := get_controls(6)
    io.CP0ToRegD  := get_controls(5)
    io.MemWriteD  := get_controls(4) 
    io.MemToRegD  := get_controls(3)
    io.LoadUnsignedD :=  get_controls(2)
    io.MemWidthD :=get_controls(1,0)   
    get_controls := Mux1H(Seq(
            ins_id(ID_NULL   ) -> CTRL_NULL,
            ins_id(ID_ADDI   ) -> CTRL_ITYPE,
            ins_id(ID_SUB    ) -> CTRL_RTYPE,
            ins_id(ID_AND    ) -> CTRL_RTYPE,
            ins_id(ID_OR     ) -> CTRL_RTYPE,
            ins_id(ID_XOR    ) -> CTRL_RTYPE,
            ins_id(ID_SLT    ) -> CTRL_RTYPE,
            ins_id(ID_SLL    ) -> CTRL_RTYPES,
            ins_id(ID_ANDI   ) -> CTRL_ITYPEU,
            ins_id(ID_ADD    ) -> CTRL_RTYPE,
            ins_id(ID_ADDU   ) -> CTRL_RTYPE,
            ins_id(ID_ADDIU  ) -> CTRL_ITYPE,
            ins_id(ID_SUBU   ) -> CTRL_RTYPE,
            ins_id(ID_SLTI   ) -> CTRL_ITYPE,
            ins_id(ID_SLTU   ) -> CTRL_RTYPE,
            ins_id(ID_SLTIU  ) -> CTRL_ITYPE,
            ins_id(ID_DIV    ) -> CTRL_DIV,
            ins_id(ID_DIVU   ) -> CTRL_DIV,
            ins_id(ID_MULT   ) -> CTRL_MULT,
            ins_id(ID_MULTU  ) -> CTRL_MULT,
            ins_id(ID_LUI    ) -> CTRL_ITYPE,
            ins_id(ID_NOR    ) -> CTRL_RTYPE,
            ins_id(ID_ORI    ) -> CTRL_ITYPEU,
            ins_id(ID_XORI   ) -> CTRL_ITYPEU,
            ins_id(ID_SLLV   ) -> CTRL_RTYPE,
            ins_id(ID_SRA    ) -> CTRL_RTYPES,
            ins_id(ID_SRAV   ) -> CTRL_RTYPE,
            ins_id(ID_SRL    ) -> CTRL_RTYPES,
            ins_id(ID_SRLV   ) -> CTRL_RTYPE,
            ins_id(ID_BEQ    ) -> CTRL_BEQ,
            ins_id(ID_BNE    ) -> CTRL_BNE,
            ins_id(ID_BGEZ   ) -> CTRL_BGEZ,
            ins_id(ID_BGEZAL ) -> CTRL_BGEZAL,
            ins_id(ID_BGTZ   ) -> CTRL_BGTZ,
            ins_id(ID_BLEZ   ) -> CTRL_BLEZ,
            ins_id(ID_BLTZ   ) -> CTRL_BLTZ,
            ins_id(ID_BLTZAL ) -> CTRL_BLTZAL,
            ins_id(ID_J      ) -> CTRL_J,
            ins_id(ID_JAL    ) -> CTRL_JAL,
            ins_id(ID_JR     ) -> CTRL_JR,
            ins_id(ID_JALR   ) -> CTRL_JALR,
            ins_id(ID_MFHI   ) -> CTRL_MFHI,
            ins_id(ID_MFLO   ) -> CTRL_MFLO,
            ins_id(ID_MTHI   ) -> CTRL_MTHI,
            ins_id(ID_MTLO   ) -> CTRL_MTLO,
            ins_id(ID_BREAK  ) -> CTRL_BREAK,
            ins_id(ID_SYSCALL) -> CTRL_SYSCALL,
            ins_id(ID_LB     ) -> CTRL_LB,
            ins_id(ID_LBU    ) -> CTRL_LBU,
            ins_id(ID_LH     ) -> CTRL_LH,
            ins_id(ID_LHU    ) -> CTRL_LHU,
            ins_id(ID_LW     ) -> CTRL_LW,
            ins_id(ID_SB     ) -> CTRL_SB,
            ins_id(ID_SH     ) -> CTRL_SH,
            ins_id(ID_SW     ) -> CTRL_SW,
            ins_id(ID_ERET   ) -> CTRL_ERET,
            ins_id(ID_MFC0   ) -> CTRL_MFC0,
            ins_id(ID_MTC0   ) -> CTRL_MTC0,
            ins_id(ID_SWL     ) -> CTRL_SWL,
            ins_id(ID_SWR   )  -> CTRL_SWR,
            ins_id(ID_LWL   ) -> CTRL_LWL,
            ins_id(ID_LWR   ) -> CTRL_LWR
    ))

    val get_alu_op = Wire(UInt(24.W))
    io.ALUCtrlD  := Mux(reset.asBool,1.U<<ALU_NULL,get_alu_op)

    get_alu_op := Mux1H(Seq(
        ins_id(ID_NULL    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_ADD     )  ->(1.U<<ALU_ADDE) ,
        ins_id(ID_ADDI    )  ->(1.U<<ALU_ADDE) ,
        ins_id(ID_ADDU    )  ->(1.U<<ALU_ADDU) ,
        ins_id(ID_ADDIU   )  ->(1.U<<ALU_ADDU) ,
        ins_id(ID_SUB     )  ->(1.U<<ALU_SUBE) ,
        ins_id(ID_SUBU    )  ->(1.U<<ALU_SUBU) ,
        ins_id(ID_SLT     )  ->(1.U<<ALU_SLT) ,
        ins_id(ID_SLTI    )  ->(1.U<<ALU_SLT) ,
        ins_id(ID_SLTU    )  ->(1.U<<ALU_SLTU) ,
        ins_id(ID_SLTIU   )  ->(1.U<<ALU_SLTU) ,
        ins_id(ID_DIV     )  ->(1.U<<ALU_DIV) ,
        ins_id(ID_DIVU    )  ->(1.U<<ALU_DIVU) ,
        ins_id(ID_MULT    )  ->(1.U<<ALU_MULT) ,
        ins_id(ID_MULTU   )  ->(1.U<<ALU_MULTU) ,
        ins_id(ID_AND     )  ->(1.U<<ALU_AND) ,
        ins_id(ID_ANDI    )  ->(1.U<<ALU_AND) ,
        ins_id(ID_LUI     )  ->(1.U<<ALU_LUI) ,
        ins_id(ID_NOR     )  ->(1.U<<ALU_NOR) ,
        ins_id(ID_OR      )  ->(1.U<<ALU_OR) ,
        ins_id(ID_ORI     )  ->(1.U<<ALU_OR) ,
        ins_id(ID_XOR     )  ->(1.U<<ALU_XOR) ,
        ins_id(ID_XORI    )  ->(1.U<<ALU_XOR) ,
        ins_id(ID_SLL     )  ->(1.U<<ALU_SLL) ,
        ins_id(ID_SLLV    )  ->(1.U<<ALU_SLL) ,
        ins_id(ID_SRA     )  ->(1.U<<ALU_SRA) ,
        ins_id(ID_SRAV    )  ->(1.U<<ALU_SRA) ,
        ins_id(ID_SRL     )  ->(1.U<<ALU_SRL) ,
        ins_id(ID_SRLV    )  ->(1.U<<ALU_SRL) ,
        ins_id(ID_BEQ     )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_BNE     )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_BGEZ    )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_BGEZAL  )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_BGTZ    )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_BLEZ    )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_BLTZ    )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_BLTZAL  )  ->(1.U<<ALU_SUB) ,
        ins_id(ID_J       )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_JAL     )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_JR      )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_JALR    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_MFHI    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_MFLO    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_MTHI    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_MTLO    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_BREAK   )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_SYSCALL )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_LB      )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_LBU     )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_LH      )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_LHU     )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_LW      )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_SB      )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_SH      )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_SW      )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_SWL     )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_SWR     )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_LWL     )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_LWR     )  ->(1.U<<ALU_ADD) ,
        ins_id(ID_ERET    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_MFC0    )  ->(1.U<<ALU_NULL) ,
        ins_id(ID_MTC0    )  ->(1.U<<ALU_NULL)        
    ))
}


object cu_test extends App{
    (new ChiselStage).emitVerilog(new cu)
}



class cu extends Module with mips_macros {
    val io1 = IO(new Bundle{
        val   InstrD = Input(UInt(32.W))
        val   BadInstrD=      Output(UInt(1.W))
        val   BreakD=      Output(UInt(1.W))
        val   SysCallD=      Output(UInt(1.W))
        val   EretD   =   Output(UInt(1.W))
    })   
    val io = IO(new decoder_port )

    
    val OpD = io1.InstrD(31,26)//首字母为o的大写(>_<)
    val FunctD = io1.InstrD(5,0)
    val RsD    = io1.InstrD(25,21)
    val RtD    = io1.InstrD(20,16)
    val ins_id = Wire(UInt(7.W))


    // io.BranchD_Flag := Mux(OpD === )
    ins_id := MuxLookup(OpD,ID_NULL.U,Seq(
        ( OP_ADDI) -> (ID_ADDI).U,
        ( OP_ANDI) -> (ID_ANDI).U,
        ( OP_ADDIU) -> (ID_ADDIU).U,
        ( OP_SLTI) -> (ID_SLTI).U,
        ( OP_SLTIU) -> (ID_SLTIU).U,
        ( OP_LUI) -> (ID_LUI).U,
        ( OP_ORI) -> (ID_ORI).U,
        ( OP_XORI) -> (ID_XORI).U,
        ( OP_BEQ) -> (ID_BEQ).U,
        ( OP_BNE ) -> (ID_BNE.U ),
        ( OP_BGTZ) -> (ID_BGTZ.U),
        ( OP_BLEZ) -> (ID_BLEZ).U,
        ( OP_J   ) -> (ID_J.U ),
        ( OP_JAL) -> (ID_JAL.U),
        ( OP_LB) -> (ID_LB.U),
        ( OP_LBU) -> (ID_LBU.U),
        ( OP_LH) -> (ID_LH.U),
        ( OP_LHU ) -> (ID_LHU.U ),
        ( OP_LW ) ->(ID_LW.U),
        ( OP_SB) -> (ID_SB.U),
        ( OP_SH) -> (ID_SH.U),
        ( OP_SW) -> (ID_SW.U),
        (OP_LWL ) -> (ID_LWL).U,
        (OP_LWR ) -> (ID_LWR).U,
        (OP_SWL ) -> (ID_SWL).U,
        (OP_SWR ) -> (ID_SWR).U,
        ( OP_SPECIAL) -> MuxLookup(FunctD,ID_NULL.U,Seq( // 在op相同情况下，根据funct来判断是哪一条指令 这个得写特判
            ( FUNC_SUB) ->   (ID_SUB).U,
            ( FUNC_AND ) ->   (ID_AND ).U,
            ( FUNC_OR) ->   (ID_OR).U,
            ( FUNC_SLT) ->   (ID_SLT).U,
            ( FUNC_SLL) ->   (ID_SLL).U,
            ( FUNC_SLTU ) ->   (ID_SLTU ).U,
            ( FUNC_XOR) ->   (ID_XOR).U,
            ( FUNC_ADD) ->   (ID_ADD).U,
            ( FUNC_ADDU ) ->   (ID_ADDU ).U,
            ( FUNC_SUBU ) ->   (ID_SUBU ).U,
            ( FUNC_DIV) ->   (ID_DIV).U,
            ( FUNC_DIVU) ->   (ID_DIVU).U,
            ( FUNC_MULT) ->   (ID_MULT).U,
            ( FUNC_MULTU) ->   (ID_MULTU).U,
            ( FUNC_NOR) ->   (ID_NOR).U,
            ( FUNC_SLLV ) ->   (ID_SLLV ).U,
            ( FUNC_SRA) ->   (ID_SRA).U,
            ( FUNC_SRAV) ->   (ID_SRAV).U,
            ( FUNC_SRL) ->   (ID_SRL).U,
            ( FUNC_SRLV) ->   (ID_SRLV ).U,
            ( FUNC_JR) ->   (ID_JR).U,
            ( FUNC_JALR) ->   (ID_JALR.U),
            ( FUNC_MFHI ) ->   (ID_MFHI.U ),
            ( FUNC_MFLO) ->   (ID_MFLO.U ),
            ( FUNC_MTHI) ->   (ID_MTHI.U),
            ( FUNC_MTLO) ->   (ID_MTLO.U),
            ( FUNC_BREAK ) ->   (ID_BREAK.U ),
            ( FUNC_SYSCALL ) ->   (ID_SYSCALL.U )
            )),
        ( OP_BRANCH) -> MuxCase(ID_NULL.U,Seq( //后面这里可以改,在id时就开始算分支
            (RtD === RT_BGEZ )  -> (ID_BGEZ.U ),
            (RtD === RT_BGEZAL )  -> (ID_BGEZAL.U ),
            (RtD === RT_BLTZ )  -> (ID_BLTZ.U ),
            (RtD === RT_BLTZAL )  -> (ID_BLTZAL.U )
        )),
        ( OP_PRIVILEGE) -> MuxCase(ID_NULL.U,Array( //后面这里可以改,在id时就开始算分支
            (RsD === RS_ERET )  -> (ID_ERET.U ),
            (RsD === RS_MFC0 )  -> (ID_MFC0.U ),
            (RsD === RS_MTC0 )  -> (ID_MTC0.U )
        ))

    ))

    io1.BadInstrD := ins_id === (ID_NULL ).U
    io1.BreakD    := ins_id === (ID_BREAK).U
    io1.SysCallD  := ins_id === (ID_SYSCALL).U
    io1.EretD     := ins_id === (ID_ERET).U

    val get_controls = Wire(UInt(29.W))
    io.MemRLD       := get_controls(28,27)
    io.BranchD_Flag := get_controls(26)
    io.RegWriteD := get_controls(25)   //实在不知道咋写呜呜呜
    io.RegDstD   := get_controls(24,23) 
    io.ALUSrcD   := get_controls(22,21)
    io.ImmUnsigned := get_controls(20) 
    io.BranchD   := get_controls(19,14)
    io.JumpD     := get_controls(13)
    io.JRD       := get_controls(12) 
    io.LinkD     := get_controls(11)
    io.HiLoWriteD:= get_controls(10,9)
    io.HiLoToRegD := get_controls(8,7)
    io.CP0WriteD  := get_controls(6)
    io.CP0ToRegD  := get_controls(5)
    io.MemWriteD  := get_controls(4) 
    io.MemToRegD  := get_controls(3)
    io.LoadUnsignedD :=  get_controls(2)
    io.MemWidthD :=get_controls(1,0)   
    get_controls := MuxLookup(ins_id,0.U,Seq(
            (ID_NULL   ).U -> CTRL_NULL,
            (ID_ADDI   ).U -> CTRL_ITYPE,
            (ID_SUB    ).U -> CTRL_RTYPE,
            (ID_AND    ).U -> CTRL_RTYPE,
            (ID_OR     ).U -> CTRL_RTYPE,
            (ID_XOR    ).U -> CTRL_RTYPE,
            (ID_SLT    ).U -> CTRL_RTYPE,
            (ID_SLL    ).U -> CTRL_RTYPES,
            (ID_ANDI   ).U -> CTRL_ITYPEU,
            (ID_ADD    ).U -> CTRL_RTYPE,
            (ID_ADDU   ).U -> CTRL_RTYPE,
            (ID_ADDIU  ).U -> CTRL_ITYPE,
            (ID_SUBU   ).U -> CTRL_RTYPE,
            (ID_SLTI   ).U -> CTRL_ITYPE,
            (ID_SLTU   ).U -> CTRL_RTYPE,
            (ID_SLTIU  ).U -> CTRL_ITYPE,
            (ID_DIV    ).U -> CTRL_DIV,
            (ID_DIVU   ).U -> CTRL_DIV,
            (ID_MULT   ).U -> CTRL_MULT,
            (ID_MULTU  ).U -> CTRL_MULT,
            (ID_LUI    ).U -> CTRL_ITYPE,
            (ID_NOR    ).U -> CTRL_RTYPE,
            (ID_ORI    ).U -> CTRL_ITYPEU,
            (ID_XORI   ).U -> CTRL_ITYPEU,
            (ID_SLLV   ).U -> CTRL_RTYPE,
            (ID_SRA    ).U -> CTRL_RTYPES,
            (ID_SRAV   ).U -> CTRL_RTYPE,
            (ID_SRL    ).U -> CTRL_RTYPES,
            (ID_SRLV   ).U -> CTRL_RTYPE,
            (ID_BEQ    ).U -> CTRL_BEQ,
            (ID_BNE    ).U -> CTRL_BNE,
            (ID_BGEZ   ).U -> CTRL_BGEZ,
            (ID_BGEZAL ).U -> CTRL_BGEZAL,
            (ID_BGTZ   ).U -> CTRL_BGTZ,
            (ID_BLEZ   ).U -> CTRL_BLEZ,
            (ID_BLTZ   ).U -> CTRL_BLTZ,
            (ID_BLTZAL ).U -> CTRL_BLTZAL,
            (ID_J      ).U -> CTRL_J,
            (ID_JAL    ).U -> CTRL_JAL,
            (ID_JR     ).U -> CTRL_JR,
            (ID_JALR   ).U -> CTRL_JALR,
            (ID_MFHI   ).U -> CTRL_MFHI,
            (ID_MFLO   ).U -> CTRL_MFLO,
            (ID_MTHI   ).U -> CTRL_MTHI,
            (ID_MTLO   ).U -> CTRL_MTLO,
            (ID_BREAK  ).U -> CTRL_BREAK,
            (ID_SYSCALL).U -> CTRL_SYSCALL,
            (ID_LB     ).U -> CTRL_LB,
            (ID_LBU    ).U -> CTRL_LBU,
            (ID_LH     ).U -> CTRL_LH,
            (ID_LHU    ).U -> CTRL_LHU,
            (ID_LW     ).U -> CTRL_LW,
            (ID_SB     ).U -> CTRL_SB,
            (ID_SH     ).U -> CTRL_SH,
            (ID_SW     ).U -> CTRL_SW,
            (ID_ERET   ).U -> CTRL_ERET,
            (ID_MFC0   ).U -> CTRL_MFC0,
            (ID_MTC0   ).U -> CTRL_MTC0,
            (ID_SWL     .U) -> CTRL_SWL,
            (ID_SWR   ) .U -> CTRL_SWR,
            (ID_LWL   ) .U-> CTRL_LWL,
            (ID_LWR   ) .U-> CTRL_LWR
    ))

    val get_alu_op = Wire(UInt(24.W))
    io.ALUCtrlD  := Mux(reset.asBool,1.U<<ALU_NULL,get_alu_op)

    get_alu_op := MuxLookup(ins_id,0.U,Seq(
        (ID_NULL    ).U  ->(1.U<<ALU_NULL) ,
        (ID_ADD     ).U  ->(1.U<<ALU_ADDE) ,
        (ID_ADDI    ).U  ->(1.U<<ALU_ADDE) ,
        (ID_ADDU    ).U  ->(1.U<<ALU_ADDU) ,
        (ID_ADDIU   ).U  ->(1.U<<ALU_ADDU) ,
        (ID_SUB     ).U  ->(1.U<<ALU_SUBE) ,
        (ID_SUBU    ).U  ->(1.U<<ALU_SUBU) ,
        (ID_SLT     ).U  ->(1.U<<ALU_SLT) ,
        (ID_SLTI    ).U  ->(1.U<<ALU_SLT) ,
        (ID_SLTU    ).U  ->(1.U<<ALU_SLTU) ,
        (ID_SLTIU   ).U  ->(1.U<<ALU_SLTU) ,
        (ID_DIV     ).U  ->(1.U<<ALU_DIV) ,
        (ID_DIVU    ).U  ->(1.U<<ALU_DIVU) ,
        (ID_MULT    ).U  ->(1.U<<ALU_MULT) ,
        (ID_MULTU   ).U  ->(1.U<<ALU_MULTU) ,
        (ID_AND     ).U  ->(1.U<<ALU_AND) ,
        (ID_ANDI    ).U  ->(1.U<<ALU_AND) ,
        (ID_LUI     ).U  ->(1.U<<ALU_LUI) ,
        (ID_NOR     ).U  ->(1.U<<ALU_NOR) ,
        (ID_OR      ).U  ->(1.U<<ALU_OR) ,
        (ID_ORI     ).U  ->(1.U<<ALU_OR) ,
        (ID_XOR     ).U  ->(1.U<<ALU_XOR) ,
        (ID_XORI    ).U  ->(1.U<<ALU_XOR) ,
        (ID_SLL     ).U  ->(1.U<<ALU_SLL) ,
        (ID_SLLV    ).U  ->(1.U<<ALU_SLL) ,
        (ID_SRA     ).U  ->(1.U<<ALU_SRA) ,
        (ID_SRAV    ).U  ->(1.U<<ALU_SRA) ,
        (ID_SRL     ).U  ->(1.U<<ALU_SRL) ,
        (ID_SRLV    ).U  ->(1.U<<ALU_SRL) ,
        (ID_BEQ     ).U  ->(1.U<<ALU_SUB) ,
        (ID_BNE     ).U  ->(1.U<<ALU_SUB) ,
        (ID_BGEZ    ).U  ->(1.U<<ALU_SUB) ,
        (ID_BGEZAL  ).U  ->(1.U<<ALU_SUB) ,
        (ID_BGTZ    ).U  ->(1.U<<ALU_SUB) ,
        (ID_BLEZ    ).U  ->(1.U<<ALU_SUB) ,
        (ID_BLTZ    ).U  ->(1.U<<ALU_SUB) ,
        (ID_BLTZAL  ).U  ->(1.U<<ALU_SUB) ,
        (ID_J       ).U  ->(1.U<<ALU_NULL) ,
        (ID_JAL     ).U  ->(1.U<<ALU_NULL) ,
        (ID_JR      ).U  ->(1.U<<ALU_NULL) ,
        (ID_JALR    ).U  ->(1.U<<ALU_NULL) ,
        (ID_MFHI    ).U  ->(1.U<<ALU_NULL) ,
        (ID_MFLO    ).U  ->(1.U<<ALU_NULL) ,
        (ID_MTHI    ).U  ->(1.U<<ALU_NULL) ,
        (ID_MTLO    ).U  ->(1.U<<ALU_NULL) ,
        (ID_BREAK   ).U  ->(1.U<<ALU_NULL) ,
        (ID_SYSCALL ).U  ->(1.U<<ALU_NULL) ,
        (ID_LB      ).U  ->(1.U<<ALU_ADD) ,
        (ID_LBU     ).U  ->(1.U<<ALU_ADD) ,
        (ID_LH      ).U  ->(1.U<<ALU_ADD) ,
        (ID_LHU     ).U  ->(1.U<<ALU_ADD) ,
        (ID_LW      ).U  ->(1.U<<ALU_ADD) ,
        (ID_SB      ).U  ->(1.U<<ALU_ADD) ,
        (ID_SH      ).U  ->(1.U<<ALU_ADD) ,
        (ID_SW      ).U  ->(1.U<<ALU_ADD) ,
        (ID_SWL     ).U  ->(1.U<<ALU_ADD) ,
        (ID_SWR     ).U  ->(1.U<<ALU_ADD) ,
        (ID_LWL     ).U  ->(1.U<<ALU_ADD) ,
        (ID_LWR     ).U  ->(1.U<<ALU_ADD) ,
        (ID_ERET    ).U  ->(1.U<<ALU_NULL) ,
        (ID_MFC0    ).U  ->(1.U<<ALU_NULL) ,
        (ID_MTC0    ).U  ->(1.U<<ALU_NULL)        
    ))
}


object cu1_test extends App{
    (new ChiselStage).emitVerilog(new cu_1)
}


