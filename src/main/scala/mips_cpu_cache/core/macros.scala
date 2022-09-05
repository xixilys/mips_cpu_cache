package examples

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import firrtl.PrimOps
import scala.math._
import scala.reflect.runtime.Macros

trait mips_macros {

val tlb_length = 16
val tlb_index_width = (log10(tlb_length)/log10(2)).toInt


    // inst & func def



val OP_ADDI  = "b001000".U(6.W)
val OP_ADDIU = "b001001".U(6.W)
val OP_SLTI  = "b001010".U(6.W)
val OP_SLTIU = "b001011".U(6.W)
val OP_ANDI ="b001100".U(6.W)
val OP_LUI = "b001111".U(6.W)
val OP_ORI = "b001101".U(6.W)
val OP_XORI = "b001110".U(6.W)
val OP_BEQ = "b000100".U(6.W)
val OP_BNE = "b000101".U(6.W)
val OP_BGTZ = "b000111".U(6.W)
val OP_BLEZ = "b000110".U(6.W)
val OP_J = "b000010".U(6.W)
val OP_JAL = "b000011".U(6.W)
val OP_LB = "b100000".U(6.W)
val OP_LBU = "b100100".U(6.W)
val OP_LH = "b100001".U(6.W)
val OP_LHU = "b100101".U(6.W)
val OP_LW = "b100011".U(6.W)
val OP_SB = "b101000".U(6.W)
val OP_SH = "b101001".U(6.W)
val OP_SW = "b101011".U(6.W)
val OP_LWL = "b100010".U(6.W)
val OP_LWR = "b100110".U(6.W)
val OP_SWL = "b101010".U(6.W)
val OP_SWR = "b101110".U(6.W)

val OP_SPECIAL = "b000000".U(6.W)
val OP_BRANCH = "b000001".U(6.W)
val OP_PRIVILEGE = "b010000".U(6.W)
val OP_SPECIAL2 = "b011100".U(6.W)
val OP_CACHE = "b101111".U(6.W) //cache指令，后面应该是只实现了其中几个，后续仔细讨论
val OP_REGIMM = "b000001".U(6.W)


val FUNC_ADD = "b100000".U(6.W)
val FUNC_ADDU = "b100001".U(6.W)
val FUNC_SUB = "b100010".U(6.W)
val FUNC_SUBU = "b100011".U(6.W)
val FUNC_SLT = "b101010".U(6.W)
val FUNC_SLTU = "b101011".U(6.W)
val FUNC_DIV = "b011010".U(6.W)
val FUNC_DIVU = "b011011".U(6.W)
val FUNC_MULT = "b011000".U(6.W)
val FUNC_MULTU = "b011001".U(6.W)
val FUNC_AND = "b100100".U(6.W)
val FUNC_NOR = "b100111".U(6.W)
val FUNC_OR = "b100101".U(6.W)
val FUNC_XOR = "b100110".U(6.W)
val FUNC_SLL = "b000000".U(6.W)
val FUNC_SLLV = "b000100".U(6.W)
val FUNC_SRA = "b000011".U(6.W)
val FUNC_SRAV = "b000111".U(6.W)
val FUNC_SRL = "b000010".U(6.W)
val FUNC_SRLV = "b000110".U(6.W)
val FUNC_JR = "b001000".U(6.W)
val FUNC_JALR = "b001001".U(6.W)
val FUNC_MFHI = "b010000".U(6.W)
val FUNC_MFLO = "b010010".U(6.W)
val FUNC_MTHI = "b010001".U(6.W)
val FUNC_MTLO = "b010011".U(6.W)
val FUNC_BREAK = "b001101".U(6.W)
val FUNC_SYSCALL = "b001100".U(6.W)
val FUNC_SYNC   = "b001111".U

val FUNC_TEQ = "b110100".U(6.W)
val FUNC_TNE = "b110110".U(6.W)
val FUNC_TGE = "b110000".U(6.W) //大于等于就trap    as 有符号整数
val FUNC_TGEU = "b110001".U(6.W) //大于等于就trap    as 无符号整数
val FUNC_TLT = "b110010".U(6.W) //小与就trap    as 有符号整数
val FUNC_TLTU = "b110011".U(6.W) //小于就trap    as 无符号整数



val FUNC_MUL  = "b000010".U(6.W) // 乘法，结果放到普通寄存器里面去
val RT_BGEZ = "b00001".U(5.W)
val RT_BGEZAL = "b10001".U(5.W)
val RT_BLTZ = "b00000".U(5.W)
val RT_BLTZAL = "b10000".U(5.W)
val RS_ERET = "b10000".U(5.W)
val FULL_ERET =  "b01000010_00000000_00000000_00011000".U(32.W)
val RS_MFC0 = "b00000".U(5.W)
val RS_MTC0 = "b00100".U(5.W)


//REGIMM 以下

val RT_TEQI = "b01100".U(5.W) //如果相等则trap
val RT_TNEI = "b01110".U(5.W)
val RT_TGEI = "b01000".U(5.W) //大于等于就trap , as 有符号整数
val RT_TGEIU = "b01001".U(5.W) //大于等于就trap , as 无符号整数
val RT_TLTI = "b01010".U(5.W) //小于就trap , as 有符号整数
val RT_TLTIU = "b01011".U(5.W) //小于就trap , as 无符号整数



//特权指令🏇
val CO_SET = 1.U(1.W)
val CO_RESET = 0.U(1.W)
// val RS_ERET = "b10000".U(5.W)
//co_reset
val COP_MFC0 = "b0000".U(5.W)
val COP_MTC0 = "b0100".U(5.W)
//co_set
val FUNC_TLBP =  "b001000".U(6.W)
val FUNC_TLBR =  "b000001".U(6.W)
val FUNC_TLBWI = "b000010".U(6.W)
val FUNC_TLBWR = "b000110".U(6.W)
val FUNC_ERET = "b011000".U(6.W)
val FUNC_WAIT = "b100000".U(6.W)


// // inst_type id def    "b00_10".U -> Cat(0.U(16.W),data(15,0)),
//             "b00_11".U -> data,
//             "b01_01".U -> Cat(0.U(16.W),data(7,0),0.U(8.W)),
//             // "b01_10".U -> Cat(0.U(8.W),data(15,0),0.U(8.W)),//SH和LH只能读高两位或者低两位
//             "b10_01".U -> Cat(0.U(8.W),data(7,0),0.U(16.W)),
//             "b10_10".U -> Cat(data(15,0),0.U(16.W)),
//             "b11_01".U -> Cat(data(7,0),0.U(24.W))
val ID_NULL= 0
val ID_ADD =1
val ID_ADDI =2
val ID_ADDU =3
val ID_ADDIU= 4
val ID_SUB =5
val ID_SUBU= 6
val ID_SLT =7
val ID_SLTI= 8
val ID_SLTU= 9
val ID_SLTIU= 10
val ID_DIV= 11
val ID_DIVU =12
val ID_MULT =13
val ID_MULTU =14
val ID_AND =15
val ID_ANDI =16
val ID_LUI= 17
val ID_NOR= 18
val ID_OR =19
val ID_ORI= 20
val ID_XOR =21
val ID_XORI= 22
val ID_SLL= 23
val ID_SLLV =24
val ID_SRA= 25
val ID_SRAV= 26
val ID_SRL =27
val ID_SRLV =28
val ID_BEQ =29
val ID_BNE =30
val ID_BGEZ= 31
val ID_BGEZAL= 32
val ID_BGTZ =33
val ID_BLEZ= 34
val ID_BLTZ= 35
val ID_BLTZAL= 36
val ID_J= 37
val ID_JAL =38
val ID_JR =39
val ID_JALR =40
val ID_MFHI =41
val ID_MFLO= 42
val ID_MTHI =43
val ID_MTLO= 44
val ID_BREAK= 45
val ID_SYSCALL= 46
val ID_LB =47
val ID_LBU =48
val ID_LH =49
val ID_LHU= 50
val ID_LW= 51
val ID_SB =52
val ID_SH =53
val ID_SW =54
val ID_ERET =55
val ID_MFC0= 56
val ID_MTC0= 57
val ID_NOP= 58
val ID_LWL=59
val ID_LWR=60
val ID_SWL=61
val ID_SWR=62

val ID_MUL   = 63
val ID_TLBP  = 64
val ID_TLBR  = 65
val ID_TLBWI = 66
val ID_TLBWR = 67
val ID_CACHE = 68

val ID_TEQ  = 70
val ID_TEQI = 71
val ID_TNE  = 72 
val ID_TNEI = 73
val ID_TGE  = 74
val ID_TGEI = 75
val ID_TGEU = 76
val ID_TGEIU= 77
val ID_TLT  = 78
val ID_TLTI = 79
val ID_TLTU = 80
val ID_TLTIU = 81

val ID_SYNC = 82


// alu cmd def
val ALU_NULL = 0
val ALU_ADD  = 1
val ALU_ADDE = 2
val ALU_ADDU = 3
val ALU_AND  = 4
val ALU_DIV  = 5
val ALU_DIVU = 6
val ALU_LUI  = 7
val ALU_MULT = 8
val ALU_MULTU= 9
val ALU_NOR  = 10
val ALU_OR   = 11
val ALU_SLL  = 12
val ALU_SLT  = 13
val ALU_SLTU = 14
val ALU_SRA  = 15
val ALU_SRL  = 16
val ALU_SUB  = 17
val ALU_SUBE = 18
val ALU_SUBU = 19
val ALU_XOR  = 20
val ALU_MUL  = 21





// cu control signals def
// MemRL(2)  BranchD_Flag  RegWriteD(1)	RegDstD(2)	ALUSrcD(2)	ImmUnsigned(1) BranchD(6)	JumpD(1)	JRD(1)	LinkD(1)	
// HiLoWriteD(2)  HiLoToRegD(2)	CP0WriteD(1) CP0ToRegD(1) MemWriteD(1)	MemToRegD(1) LoadUnsignedD(1)	MemWidthD(2)
val CTRL_NULL  =  "b00_0_0_00_00_0_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_ITYPE =  "b00_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)//JR用于表示是不是JR或者JALR指令，该指令储存在寄存器中
val CTRL_ITYPEU=  "b00_0_1_00_01_1_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_RTYPE =  "b00_0_1_01_00_0_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_RTYPES = "b00_0_1_01_10_0_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_LB  =    "b00_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_1_0_00".U(29.W)
val CTRL_LBU =    "b00_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_1_1_00".U(29.W)
val CTRL_LH  =    "b00_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_1_0_01".U(29.W)
val CTRL_LHU=     "b00_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_1_1_01".U(29.W)
val CTRL_LW =     "b00_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_1_0_10".U(29.W)
val CTRL_LWL =    "b10_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_1_0_10".U(29.W)
val CTRL_LWR =    "b01_0_1_00_01_0_000000_0_0_0_00_00_0_0_0_1_0_10".U(29.W)
val CTRL_SB =     "b00_0_0_00_01_0_000000_0_0_0_00_00_0_0_1_0_0_00".U(29.W)
val CTRL_SH =     "b00_0_0_00_01_0_000000_0_0_0_00_00_0_0_1_0_0_01".U(29.W)
val CTRL_SW =     "b00_0_0_00_01_0_000000_0_0_0_00_00_0_0_1_0_0_10".U(29.W)
val CTRL_SWL =    "b10_0_0_00_01_0_000000_0_0_0_00_00_0_0_1_0_0_10".U(29.W)
val CTRL_SWR =    "b01_0_0_00_01_0_000000_0_0_0_00_00_0_0_1_0_0_10".U(29.W)
val CTRL_BEQ =    "b00_1_0_00_00_0_000001_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_BNE =    "b00_1_0_00_00_0_000010_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_BGEZ =   "b00_1_0_00_00_0_000100_0_0_0_00_00_0_0_0_0_0_00".U(29.W)//branch 部分仅仅用来代表是大于跳转还是小于跳转
val CTRL_BGEZAL = "b00_1_1_10_00_0_000100_0_0_1_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_BGTZ =   "b00_1_0_00_00_0_001000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_BLEZ =   "b00_1_0_00_00_0_010000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_BLTZ =   "b00_1_0_00_00_0_100000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_BLTZAL = "b00_1_1_10_00_0_100000_0_0_1_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_J  =     "b00_0_0_00_00_0_000000_1_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_JAL =    "b00_0_1_10_00_0_000000_1_0_1_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_JR  =    "b00_0_0_00_00_0_000000_1_1_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_JALR =   "b00_0_1_01_00_0_000000_1_1_1_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_DIV =    "b00_0_0_00_00_0_000000_0_0_0_11_00_0_0_0_0_0_00".U(29.W)
val CTRL_DIVU =   "b00_0_0_00_00_0_000000_0_0_0_11_00_0_0_0_0_0_00".U(29.W)
val CTRL_MULT =   "b00_0_0_00_00_0_000000_0_0_0_11_00_0_0_0_0_0_00".U(29.W)
val CTRL_MULTU =  "b00_0_0_00_00_0_000000_0_0_0_11_00_0_0_0_0_0_00".U(29.W)
val CTRL_MFHI =   "b00_0_1_01_00_0_000000_0_0_0_00_10_0_0_0_0_0_00".U(29.W)
val CTRL_MFLO  =  "b00_0_1_01_00_0_000000_0_0_0_00_01_0_0_0_0_0_00".U(29.W)
val CTRL_MTHI =   "b00_0_0_00_00_0_000000_0_0_0_10_00_0_0_0_0_0_00".U(29.W)
val CTRL_MTLO =   "b00_0_0_00_00_0_000000_0_0_0_01_00_0_0_0_0_0_00".U(29.W)
val CTRL_BREAK =  "b00_0_0_00_00_0_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_SYSCALL ="b00_0_0_00_00_0_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_ERET =   "b00_0_0_00_00_0_000000_0_0_0_00_00_0_0_0_0_0_00".U(29.W)
val CTRL_MFC0 =   "b00_0_1_00_00_0_000000_0_0_0_00_00_0_1_0_0_0_00".U(29.W)
val CTRL_MTC0 =   "b00_0_0_00_00_0_000000_0_0_0_00_00_1_0_0_0_0_00".U(29.W)
//既需要读cp0又需要写cp0
//因为我们是在mem1阶段进行tlb的查询，如果使能了cp0write可能会导致错误的前递
val CTRL_TLBP =   "b00_0_0_00_00_0_000000_0_0_0_00_00_0_1_0_0_0_00".U(29.W)
//同样也是读cp0中的某个寄存器，但是不写寄存器拉
val CTRL_TLBR =   "b00_0_0_00_00_0_000000_0_0_0_00_00_0_1_0_0_0_00".U(29.W)
val CTRL_TLBWI =  "b00_0_0_00_00_0_000000_0_0_0_00_00_0_1_0_0_0_00".U(29.W)


// // cp0 address & select
val CP0_ADDR_SEL_INDEX      = "b00000_0".U 
val CP0_ADDR_SEL_RANDOM     = "b00001_0".U 
val CP0_ADDR_SEL_ENTRYLO0   = "b00010_0".U 
val CP0_ADDR_SEL_ENTRYLO1   = "b00011_0".U 
val CP0_ADDR_SEL_PAGEMASK   = "b00101_0".U 
val CP0_ADDR_SEL_BADVADDR   = "b01000_0".U 
val CP0_ADDR_SEL_COUNT      = "b01001_0".U 
val CP0_ADDR_SEL_ENTRYHI    = "b01010_0".U 
val CP0_ADDR_SEL_COMPARE    = "b01011_0".U 
val CP0_ADDR_SEL_STATUS     = "b01100_0".U 
val CP0_ADDR_SEL_CAUSE      = "b01101_0".U 
val CP0_ADDR_SEL_EPC        = "b01110_0".U 
val CP0_ADDR_SEL_PRID       = "b01111_0".U 
val CP0_ADDR_SEL_CONFIG0    = "b10000_0".U 
val CP0_ADDR_SEL_CONFIG1    = "b00000_1".U 
val CP0_ADDR_SEL_EBASE      = "b01111_1".U


// // exception
val EXCEP_INT       = 0x0  .U     // interrupt
val EXCEP_AdELD     = 0x4  .U     // lw addr error
val EXCEP_AdELI     = 0x14 .U     // pc fetch error
val EXCEP_AdES      = 0x5  .U     // sw addr 
val EXCEP_Sys       = 0x8  .U     // syscall
val EXCEP_Bp        = 0x9  .U     // break point
val EXCEP_RI        = 0xa  .U     // reserved instr
val EXCEP_Ov        = 0xc  .U     // overflow      
val EXCEP_Tr        = 0xd  .U     // trap
val EXCEP_ERET      = 0x1f .U     // eret treated as exception
val EXCEP_TLBInvalid_L  = 0x2 .U  
val EXCEP_TLBRefill_L   = 0x3 .U  
val EXCEP_TLBInvalid_S  = 0x7 .U
val EXCEP_TLBRefill_S   = 0x6 .U
val EXCEP_TLBModified   = 0x1 .U


// // exception mask
val EXCEP_MASK_INT           ="b00000000_00000000_00000000_00000001".U
val EXCEP_MASK_AdELD         ="b00000000_00000000_00000000_00010000".U
val EXCEP_MASK_AdELI         ="b00000000_00010000_00000000_00000000".U
val EXCEP_MASK_AdES          ="b00000000_00000000_00000000_00100000".U
val EXCEP_MASK_Sys           ="b00000000_00000000_00000001_00000000".U
val EXCEP_MASK_Bp            ="b00000000_00000000_00000010_00000000".U
val EXCEP_MASK_RI            ="b00000000_00000000_00000100_00000000".U
val EXCEP_MASK_Ov            ="b00000000_00000000_00010000_00000000".U
val EXCEP_MASK_Tr            ="b00000000_00000000_00100000_00000000".U
val EXCEP_MASK_ERET          ="b10000000_00000000_00000000_00000000".U
val EXCEP_MASK_TLBInvalid_L  ="b00000000_00000000_00000000_00000100".U
val EXCEP_MASK_TLBRefill_L   ="b00000000_00000000_00000000_00001000".U
val EXCEP_MASK_TLBInvalid_S  ="b00000000_00000000_00000000_10000000".U
val EXCEP_MASK_TLBRefill_S   ="b00000000_00000000_00000000_01000000".U
val EXCEP_MASK_TLBModified   ="b00000000_00000000_00000000_00000010".U

// // exception code
val EXCEP_CODE_INT      = 0x0 .U     // interrupt
val EXCEP_CODE_AdEL     = 0x4 .U     // pc fetch or lw addr error
val EXCEP_CODE_AdES     = 0x5 .U     // sw addr 
val EXCEP_CODE_Sys      = 0x8 .U     // syscall
val EXCEP_CODE_Bp       = 0x9 .U     // break point
val EXCEP_CODE_RI       = 0xa .U     // reserved instr
val EXCEP_CODE_Ov       = 0xc .U     // overflow      
val EXCEP_CODE_Tr       = 0xd .U     // trap
val EXCEP_CODE_ERET     = 0x1f.U     // eret treated as exception
val EXCEP_CODE_TLBL     = 0x2 .U     // tlbl, refill bfc00200, invalid bfc00380
val EXCEP_CODE_TLBS     = 0x3 .U     // tlbs, refill bfc00200, invalid bfc00380
val EXCEP_CODE_MOD      = 0x1 .U     // modified

def sign_extend(value:UInt,length:Int):UInt = 
    Cat(Cat(Seq.fill(32-length)(value(length-1))),value(length-1,0))

def unsign_extend(value:UInt,length:Int):UInt = 
    Cat(0.U(32-length),value(length-1,0))
def Mux2_4(sel:UInt,ch0:UInt,ch1:UInt,ch2:UInt,ch3:UInt):UInt = MuxLookup(sel,0.U,Seq(
    0.U -> ch0,1.U -> ch1,2.U -> ch2,3.U -> ch3))


//输入最好为4的整数倍
def Hash(num:UInt) : UInt  = {
  val length = num.getWidth 
  val num_array = Wire(Vec((length/4),UInt(1.W)))
  for(i <- 0 to (length/4)-1) {
    num_array(i) :=  num(((i+1)*4 - 1),i*4).xorR
  }
  num_array.asUInt

}
def  branch_prediction_state_machine_code_decoder(code:UInt) :Bool  = {
    MuxLookup(code,0.asUInt.asBool,Seq(
        Strongly_Taken -> 1.U.asBool,
        Strongly_Not_Taken -> 0.U.asBool,
        Weakly_Not_Taken -> 0.U.asBool,
        Weakly_Taken  -> 1.U.asBool))
} 
//branch prediction state machine code 
val Strongly_Not_Taken = "b00".U
val Strongly_Taken = "b11".U
val Weakly_Not_Taken = "b01".U
val Weakly_Taken = "b10".U

def br_state_machine_next_state(code:UInt,state:Bool) :UInt = {
    MuxLookup(code,code,Seq(
        Strongly_Not_Taken -> Mux(state,Weakly_Not_Taken,Strongly_Not_Taken),
        Strongly_Taken -> Mux(state,Strongly_Taken,Weakly_Taken),      
        Weakly_Not_Taken -> Mux(state,Weakly_Taken,Strongly_Not_Taken),   
        Weakly_Taken   -> Mux(state,Strongly_Taken,Weakly_Not_Taken)
    ))
}

def check_unaligned(width:UInt,rd:UInt,memrl:UInt):UInt = (memrl === 0.U && MuxCase(1.U,Seq(
        (Cat(width,rd(0)) === "b010".U) -> 0.U,
        (Cat(width,rd) === "b1000".U) -> 0.U,
        (width ===       0.U )->0.U )).asBool)
def check_cached(address:UInt) :UInt = address(31,29) === "b100".U
def memory_mapping(address:UInt): UInt = Mux(address(31,30) === "b10".U ,
    Cat(0.U(3.W),address(28,0)),address)//只有0x8-0x9 and 0xa-0xb为unmapped，可以直接线性映射
def check_mapped(address : UInt) :Bool = {
    !address(31) || address(31,30) === "b11".U }

    def get_cdv1(value : UInt) : UInt = {value(4,0) }
    def get_pfn1(value : UInt) : UInt = {value(24,5) }
    def get_cdv0(value : UInt) : UInt = {value(29,25) }
    def get_pfn0(value : UInt) : UInt = {value(49,30) }
    def get_g(value:UInt):Bool = { value(50) }
    def get_asid(value:UInt):UInt = {value(58,51) }
    def get_vpn2(value:UInt):UInt = {value(77,59) }

}
         
