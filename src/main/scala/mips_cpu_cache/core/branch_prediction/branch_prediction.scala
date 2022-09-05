package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import firrtl.PrimOps
import scala.math._
import scala.reflect.runtime.Macros
import javax.swing.plaf.basic.BasicToolBarUI


//流水线分支预测
class  branch_prediction   extends Module  with mips_macros{
   
    val io = IO(new Bundle { //分支指令不支持同时写
        val pc = Input(UInt(32.W)) //一般是支持同时判断两条指令pc和pc+4和pc+8，而对于处于八位边界的指令，还是和cache一样不做处理，只发射一条指令，我想要cache支持一次出三条了，草
        val pc_plus = Input(UInt(32.W)) 
        val pc_plus_plus = Input(UInt(32.W))
        val write_pc = Input(UInt(32.W))
        val aw_pht_ways_addr = Input(UInt(4.W))
        val aw_pht_addr = Input(UInt(7.W))
        val aw_bht_addr   = Input(UInt(7.W))
        val aw_target_addr = Input(UInt(32.W))
        val btb_write = Input(Bool())
        val bht_write = Input(Bool())
        val pht_write = Input(Bool()) 
        val bht_in = Input(UInt(7.W))
        val pht_in = Input(UInt(2.W))
        val out_L = Output(UInt(2.W))
        val out_M = Output(UInt(2.W))
        val out_R = Output(UInt(2.W))
        val pre_L = Output(UInt(1.W))
        val pre_M = Output(UInt(1.W))
        val pre_R = Output(UInt(1.W))
        val bht_L = Output(UInt(7.W)) 
        val bht_M = Output(UInt(7.W))
        val bht_R = Output(UInt(7.W))

        val btb_hit = Vec(3,Output(Bool()))
     
        val pre_target_L = Output(UInt(32.W))
        val pre_target_M = Output(UInt(32.W))
        val pre_target_R = Output(UInt(32.W))  
        val stage2_stall = Input(Bool())
        val stage2_flush = Input(Bool())

        val lookup_data = Vec(3,Output(UInt(7.W)))
    })//使用一//相当于要查两个表，不知道延迟会到多高
    
    val pc_hash = Hash(io.pc(19,4)) //
    val phts_banks = Module(new PHTS_banks_oneissue(128,2,16,4)).io
    val bhts_banks = Module(new BHT_banks_oneissue(128,7,4)).io//以后这些都还可以改，也不着急
    val btb_banks = Module(new BTB_banks_oneissue(256,4)).io
   // val stage_2_bht  = RegInit(VecInit(Seq.fill(3)(0.U(7.W))))
    val stage_2_pht_lookup = RegInit(VecInit(Seq.fill(3)(0.U(7.W))))
    val stage_2_pc = RegInit(0.U(32.W))
    val stage_2_pc_hash = RegInit(0.U(4.W))
    val stage_2_btb_hit = RegInit(VecInit(Seq.fill(3)(0.U.asBool)))

    io.btb_hit(0) := stage_2_btb_hit(0)
    io.btb_hit(1) := stage_2_btb_hit(1)
    io.btb_hit(2) := stage_2_btb_hit(2)

    val stage_1_pht_lookup =  Wire(Vec(3,UInt(7.W)))

    stage_2_pht_lookup(0) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,stage_1_pht_lookup(0),stage_2_pht_lookup(0)))
    stage_2_pht_lookup(1) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,stage_1_pht_lookup(1),stage_2_pht_lookup(1)))
    stage_2_pht_lookup(2) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,stage_1_pht_lookup(2),stage_2_pht_lookup(2)))

    stage_2_pc := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,io.pc,stage_2_pc))

    stage_2_pc_hash := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,pc_hash,stage_2_pc_hash))
    stage_2_btb_hit(0) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,btb_banks.hit_L, stage_2_btb_hit(0) ))
    stage_2_btb_hit(1) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,btb_banks.hit_M, stage_2_btb_hit(1) ))
    stage_2_btb_hit(2) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,btb_banks.hit_R, stage_2_btb_hit(2) ))

    io.lookup_data(0) := stage_2_pht_lookup(0)
    io.lookup_data(1) := stage_2_pht_lookup(1)
    io.lookup_data(2) := stage_2_pht_lookup(2)
          
   

    bhts_banks.ar_bank_sel := io.pc(3,2)//二路组相连嘛
    bhts_banks.write := io.bht_write
    bhts_banks.ar_addr_L := io.pc(10,4)
    bhts_banks.ar_addr_M := io.pc_plus(10,4)
    bhts_banks.ar_addr_R := io.pc_plus_plus(10,4) //长度为128的寻址
    bhts_banks.in := io.bht_in
    bhts_banks.aw_addr := io.aw_bht_addr

    phts_banks.ar_bank_sel := stage_2_pc(3,2)
    phts_banks.ar_pht_addr := stage_2_pc_hash    

    phts_banks.ar_addr_L := stage_2_pht_lookup(0)
    phts_banks.ar_addr_M := stage_2_pht_lookup(1)
    phts_banks.ar_addr_R := stage_2_pht_lookup(2)   

    stage_1_pht_lookup(0) := bhts_banks.out_L ^ io.pc(16,10)
    stage_1_pht_lookup(1) := bhts_banks.out_M ^ io.pc_plus(16,10)
    stage_1_pht_lookup(2) := bhts_banks.out_R ^ io.pc_plus_plus(16,10)


    // phts_banks.ar_addr_L := bhts_banks.out_L ^ io.pc(16,10)
    // phts_banks.ar_addr_M := bhts_banks.out_M ^ io.pc_plus(16,10)
    // phts_banks.ar_addr_R := bhts_banks.out_R ^ io.pc_plus_plus(16,10)

    phts_banks.aw_addr := io.aw_pht_addr
    phts_banks.aw_pht_addr := io.aw_pht_ways_addr
    phts_banks.aw_bank_sel := io.write_pc(3,2)
    phts_banks.write := io.pht_write
    phts_banks.in := io.pht_in


    btb_banks.ar_addr_L := io.pc
    btb_banks.ar_addr_M := io.pc_plus
    btb_banks.ar_addr_R := io.pc_plus_plus
    btb_banks.aw_addr   := io.write_pc
    btb_banks.aw_target_addr := io.aw_target_addr
    btb_banks.write := io.btb_write

    //不命中就预测不跳转



    io.out_L := phts_banks.out_L
    io.out_M := phts_banks.out_M
    io.out_R := phts_banks.out_R

    io.bht_L := bhts_banks.out_L 
    io.bht_M := bhts_banks.out_M 
    io.bht_R := bhts_banks.out_R 

    io.pre_L := branch_prediction_state_machine_code_decoder(phts_banks.out_L)
    io.pre_M := branch_prediction_state_machine_code_decoder(phts_banks.out_M) 
    io.pre_R := branch_prediction_state_machine_code_decoder(phts_banks.out_R)

    io.pre_target_L := btb_banks.out_L
    io.pre_target_M := btb_banks.out_M
    io.pre_target_R := btb_banks.out_R

    // val reg_out_1 = IO(Output(UInt(96.W)))
    // val reg_out_2 = IO(Output(UInt(9.W)))
    // val test_reg = RegInit(0.U(9.W))
    // val test_reg_1 = RegInit(0.U(96.W))
    // reg_out_1 := test_reg_1
    // reg_out_2 := test_reg
    // test_reg_1 := Cat(io.pre_target_L,io.pre_target_M,io.pre_target_R)
    // test_reg := Cat(io.out_L,io.out_M,io.out_R)

}
// object Branch_predection_test extends App{
//     (new ChiselStage).emitVerilog(new branch_prediction)
// }



//流水线分支预测
class  branch_prediction_with_blockram   extends Module  with mips_macros{
   
    val io = IO(new Bundle { //分支指令不支持同时写

        val sram_pc = Input(UInt(32.W))
        val pc = Input(UInt(32.W)) //一般是支持同时判断两条指令pc和pc+4和pc+8，而对于处于八位边界的指令，还是和cache一样不做处理，只发射一条指令，我想要cache支持一次出三条了，草
        val pc_plus = Input(UInt(32.W)) 
        val pc_plus_plus = Input(UInt(32.W))
        val write_pc = Input(UInt(32.W))
        val aw_pht_ways_addr = Input(UInt(4.W))
        val aw_pht_addr = Input(UInt(7.W))
        val aw_bht_addr   = Input(UInt(7.W))
        val aw_target_addr = Input(UInt(32.W))
        val btb_write = Input(Bool())
        val bht_write = Input(Bool())
        val pht_write = Input(Bool()) 
        val bht_in = Input(UInt(7.W))
        val pht_in = Input(UInt(8.W))
        val out_L = Output(UInt(2.W))
        val out_M = Output(UInt(2.W))
        val out_R = Output(UInt(2.W))
        val pre_L = Output(UInt(1.W))
        val pre_M = Output(UInt(1.W))
        val pre_R = Output(UInt(1.W))
        val bht_L = Output(UInt(7.W)) 
        val bht_M = Output(UInt(7.W))
        val bht_R = Output(UInt(7.W))

        val btb_hit = Vec(3,Output(Bool()))
     
        val pre_target_L = Output(UInt(32.W))
        val pre_target_M = Output(UInt(32.W))
        val pre_target_R = Output(UInt(32.W))  
        val stage2_stall = Input(Bool())
        val stage2_flush = Input(Bool())

        val pht_out = Output(UInt(8.W))

        val lookup_data = Vec(3,Output(UInt(7.W)))
    })//使用一//相当于要查两个表，不知道延迟会到多高
    
    val pc_hash = Hash(io.pc(19,4)) //
    val phts_banks = Module(new PHTS_banks_oneissue_block_ram(128,2,8,4)).io
    val bhts_banks = Module(new BHT_banks_oneissue(128,3,4)).io//以后这些都还可以改，也不着急
    val btb_banks = Module(new BTB_banks_oneissue_with_block_ram(512,4)).io
   // val stage_2_bht  = RegInit(VecInit(Seq.fill(3)(0.U(7.W))))
    val stage_2_pht_lookup = RegInit(VecInit(Seq.fill(3)(0.U(7.W))))
    val stage_2_pc = RegInit(0.U(32.W))
    val stage_2_pc_hash = RegInit(0.U(4.W))
    // val stage_2_btb_hit = RegInit(VecInit(Seq.fill(3)(0.U.asBool)))

    val stage2_stall_reg = RegInit(0.U.asBool)
    stage2_stall_reg := io.stage2_stall

    

    

    val stage_1_pht_lookup =  Wire(Vec(3,UInt(7.W)))

    stage_2_pht_lookup(0) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,stage_1_pht_lookup(0),stage_2_pht_lookup(0)))
    stage_2_pht_lookup(1) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,stage_1_pht_lookup(1),stage_2_pht_lookup(1)))
    stage_2_pht_lookup(2) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,stage_1_pht_lookup(2),stage_2_pht_lookup(2)))

    stage_2_pc := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,io.pc,stage_2_pc))

    stage_2_pc_hash := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,pc_hash,stage_2_pc_hash))
    // stage_2_btb_hit(0) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,btb_banks.hit_L, 0stage_2_btb_hit(0) ))
    // stage_2_btb_hit(1) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,btb_banks.hit_M, stage_2_btb_hit(1) ))
    // stage_2_btb_hit(2) := Mux(io.stage2_flush,0.U,Mux(io.stage2_stall,btb_banks.hit_R, stage_2_btb_hit(2) ))

    io.lookup_data(0) := stage_2_pht_lookup(0)
    io.lookup_data(1) := stage_2_pht_lookup(1)
    io.lookup_data(2) := stage_2_pht_lookup(2)
          
   

    bhts_banks.ar_bank_sel := io.pc(3,2)//二路组相连嘛
    bhts_banks.write := io.bht_write
    bhts_banks.ar_addr_L := io.pc(10,4)
    bhts_banks.ar_addr_M := io.pc(10,4)
    bhts_banks.ar_addr_R := io.pc(10,4) //长度为128的寻址
    bhts_banks.in := io.bht_in
    bhts_banks.aw_addr := io.aw_bht_addr

    phts_banks.ar_bank_sel := io.pc(3,2)
    phts_banks.ar_pht_addr := pc_hash//stage_2_pc_hash    

    phts_banks.ar_addr_L := stage_1_pht_lookup(0)
    phts_banks.ar_addr_M := stage_1_pht_lookup(1)
    phts_banks.ar_addr_R := stage_1_pht_lookup(2)   

    stage_1_pht_lookup(0) := Cat(bhts_banks.out_L ,io.pc(14,11))
    stage_1_pht_lookup(1) := stage_1_pht_lookup(0)// Cat(bhts_banks.out_L ,io.pc(13,10))//Cat(bhts_banks.out_M ,io.pc_plus(13,10))
    stage_1_pht_lookup(2) := stage_1_pht_lookup(0)//Cat(bhts_banks.out_R , io.pc_plus_plus(13,10))


    // phts_banks.ar_addr_L := bhts_banks.out_L ^ io.pc(16,10)
    // phts_banks.ar_addr_M := bhts_banks.out_M ^ io.pc_plus(16,10)
    // phts_banks.ar_addr_R := bhts_banks.out_R ^ io.pc_plus_plus(16,10)

    phts_banks.aw_addr := io.aw_pht_addr
    phts_banks.aw_pht_addr := io.aw_pht_ways_addr
    phts_banks.aw_bank_sel := io.write_pc(3,2)
    phts_banks.write := io.pht_write
    phts_banks.in := io.pht_in


    btb_banks.ar_addr_L := io.pc
    btb_banks.ar_addr_M := io.pc//io.pc_plus
    btb_banks.ar_addr_R := io.pc//io.pc_plus_plus
    btb_banks.aw_addr   := io.write_pc
    btb_banks.aw_target_addr := io.aw_target_addr
    btb_banks.write := io.btb_write

    //不命中就预测不跳转

    // val pht_reg = RegInit(0.U(2.W))
    // val target_pc_reg = RegInit(0.U(32.W))
    // val hit_reg = RegInit(0.U.asBool)
    // val pht_table_value = RegInit(0.U(8.W))

    // pht_reg := Mux(stage2_stall_reg,phts_banks.out_L,pht_reg)
    // target_pc_reg := Mux(stage2_stall_reg,btb_banks.out_L,target_pc_reg)
    // hit_reg := Mux(stage2_stall_reg,btb_banks.hit_L,hit_reg)
    // pht_table_value := Mux(stage2_stall_reg,phts_banks.pht_out,pht_table_value)


    io.out_L := phts_banks.out_L
    io.out_M := io.out_L//phts_banks.out_M
    io.out_R := io.out_L//phts_banks.out_R

    io.bht_L := bhts_banks.out_L 
    io.bht_M := io.bht_L//bhts_banks.out_M 
    io.bht_R := io.bht_L//bhts_banks.out_R 

    io.pre_L := branch_prediction_state_machine_code_decoder(io.out_L)
    io.pre_M := io.pre_L//branch_prediction_state_machine_code_decoder(phts_banks.out_M) 
    io.pre_R := io.pre_L//branch_prediction_state_machine_code_decoder(phts_banks.out_R)

    io.pre_target_L := btb_banks.out_L
    io.pre_target_M := io.pre_target_L//btb_banks.out_M
    io.pre_target_R := io.pre_target_L//btb_banks.out_R

    io.btb_hit(0) := btb_banks.hit_L
    io.btb_hit(1) := io.btb_hit(0)//btb_banks.hit_M
    io.btb_hit(2) := io.btb_hit(0)//btb_banks.hit_R

    io.pht_out := phts_banks.pht_out
//
    // val reg_out_1 = IO(Output(UInt(96.W)))
    // val reg_out_2 = IO(Output(UInt(9.W)))
    // val test_reg = RegInit(0.U(9.W))
    // val test_reg_1 = RegInit(0.U(96.W))
    // reg_out_1 := test_reg_1
    // reg_out_2 := test_reg
    // test_reg_1 := Cat(io.pre_target_L,io.pre_target_M,io.pre_target_R)
    // test_reg := Cat(io.out_L,io.out_M,io.out_R)

}
// object Branch_predection_test extends App{
//     (new ChiselStage).emitVerilog(new branch_prediction)
// }
