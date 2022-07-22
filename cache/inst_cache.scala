package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import os.write



class inst_cache  extends Module with mips_macros {
        //完全没用到chisel真正好的地方，我是废物呜呜呜呜
    val io = IO(new Bundle {
        val port = new axi_ram_port_multi_banking
        val     sram_hit   = Output(UInt(1.W))
        // val     sram_write_en  = Output(UInt(2.W))
        val     fec_1_pc_valid = Output(UInt(3.W))
       // val     cache_working_on = Output(Bool())
        val     stage2_flush = Input(Bool())
        val     stage2_stall = Output(Bool())
        val     stage1_valid_flush = Input(UInt(2.W))
        val     inst_ready_to_use = Input(Bool())
        val     inst_buffer_full = Input(Bool())
      //  val     cache_working_on  = Output(UInt(2.W))
       // val     stage_1_flush = Input(Bool())

        // val     sram_data_valid = Output(UInt(3.W))

        // val     sram_rdata_L  = Output(UInt(32.W))
        // val     sram_rdata_M  = Output(UInt(32.W))
        // val     sram_rdata_R  = Output(UInt(32.W))
        // val     sram_branch_hit = Output(UInt(2.W))
    })
    val lru = RegInit(VecInit(Seq.fill(128)(0.U(1.W))))
    // val sram_addr_reg = RegInit(0.U(32.W))
    // val sram_cache_reg = RegInit(0.U(1.W))

    // val access_cache_addr =  Mux(io.port.sram_req.asBool,io.port.sram_addr,sram_addr_reg)
    // val access_cache_state =  Mux(io.port.sram_req.asBool,io.port.sram_cache,sram_cache_reg)
    val stage1_flush = Wire(Bool())
    val stage1_stall = Wire(Bool())
    val stage2_flush = Wire(Bool())
    val stage2_stall = Wire(Bool())
    // val stage3_flush = Wire(Bool())
    // val stage3_stall = Wire(Bool())
    // val icache_L_tag_0     = Module(new icache_tag).io
    // val icache_L_tag_1     = Module(new icache_tag).io
    // val icache_R_tag_0     = Module(new icache_tag).io
    // val icache_R_tag_1     = Module(new icache_tag).io //复制两个
    val icache_tag_0     = Module(new icache_tag).io
    val icache_tag_1     = Module(new icache_tag).io


    // val write_counter_same = RegInit(0.U(1.W))
    

    // sram_addr_reg := Mux(io.port.sram_req.asBool,io.port.sram_addr,sram_addr_reg)
    // sram_cache_reg := Mux(io.port.sram_req.asBool,io.port.sram_cache,sram_cache_reg)


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

    val work_state = RegInit(state_lookup)
    val access_work_state = Wire(UInt(4.W))
    val write_counter  = RegInit(0.U(3.W))
    val wait_data_L  = RegInit(0.U(32.W))
    val wait_data_R  = RegInit(0.U(32.W))
    val wait_data_M  = RegInit(0.U(32.W))
    val hit_reg = RegInit(0.U(1.W))
    //copy from thu llcl 
    //all control signals
    stage1_flush := 0.U
    stage2_flush := io.stage2_flush
    stage1_stall := io.port.sram_req
    io.stage2_stall := stage2_stall
    //满了就停止流水线嘛
   
    

    //stage 1  打一拍存下数据sram信号，输入仅有一个信号，输出按照pc和pc+4给出
    val stage1_sram_addr_reg = RegEnable(Mux(stage1_flush,0.U,io.port.sram_addr),0.U,stage1_stall) //把这个加4的过程放到stage2，尽量减少stage1的逻辑层数
    val stage1_sram_cache_reg = RegEnable(Mux(stage1_flush,0.U,io.port.sram_cache),0.U(1.W),stage1_stall)   
    val stage1_sram_req_reg = RegEnable(Mux(stage1_flush,0.U,io.inst_ready_to_use),0.U(1.W),stage1_stall) 
    val stage1_sram_valid = RegInit(0.U(2.W))

    val stage1_finished = RegInit(0.U.asBool)

    stage1_finished := Mux(stage1_stall.asBool,0.U,Mux(work_state === state_miss_update || work_state === state_access_ram_1 ,1.U,0.U))
    val access_stage1_sram_valid =  Mux(io.stage1_valid_flush(0),1.U,Mux(/*work_state === state_data_ready || work_state === state_lookup*/0.U.asBool,0.U,
        Mux(io.stage1_valid_flush(1),2.U,stage1_sram_valid)))

    stage2_stall := (access_work_state === state_lookup  || access_work_state === state_data_ready) &&
         (!io.inst_buffer_full  ) //有分支指令导致的挂逼


    stage1_sram_valid := Mux(stage1_stall.asBool,"b11".U,access_stage1_sram_valid)
        // Mux(io.stage1_valid_flush(0),1.U,Mux(io.stage1_valid_flush(1),2.U,Mux(stage1_stall.asBool,"b11".U,stage1_sram_valid)))
        //access_stage1_sram_valid
        //access_stage1_sram_validstage1_sram_valid
    //stage 2  比较tag，给出hit信号,同步向sram发出读数据信号
    //同时流水线中该阶段在进行分支预测的计算    C
    // icache_L_tag_0.op     := 0.U
    // icache_L_tag_1.op     := 0.U
    // icache_R_tag_0.op     := 0.U
    // icache_R_tag_1.op     := 0.U
    
    // icache_L_tag_0.addr   := stage1_sram_addr_reg
    // icache_L_tag_1.addr   := stage1_sram_addr_reg
    // icache_R_tag_0.addr   := stage1_sram_addr_reg + 4.U
    // icache_R_tag_1.addr   := stage1_sram_addr_reg + 4.U

    icache_tag_0.op     := 0.U
    icache_tag_1.op     := 0.U
    
    icache_tag_0.addr   := stage1_sram_addr_reg
    icache_tag_1.addr   := stage1_sram_addr_reg


    for(i <- 0 to 7 ) {
        icache_data_way0(i).addr := stage1_sram_addr_reg
        icache_data_way0(i).wdata := io.port.rdata
        icache_data_way0(i).en := 1.U
    }
    for(i <- 0 to 7 ) {
        icache_data_way1(i).addr := stage1_sram_addr_reg
        icache_data_way1(i).wdata := io.port.rdata
        icache_data_way1(i).en := 1.U
    }

    val hit = (icache_tag_0.hit.asBool && icache_tag_0.valid.asBool) || 
        (icache_tag_1.hit.asBool && icache_tag_1.valid.asBool)

    lru(stage1_sram_addr_reg(11,5)) := Mux(access_work_state === state_lookup,
        Mux(icache_tag_0.hit.asBool /*&& icache_tag_0.valid.asBool*/,1.U.asBool,
        Mux(icache_tag_1.hit.asBool /*&& icache_tag_1.valid.asBool*/,0.U.asBool,lru(stage1_sram_addr_reg(11,5)) )),
        Mux(work_state === state_miss_update,~lru(stage1_sram_addr_reg(11,5)),  lru(stage1_sram_addr_reg(11,5)) ))

    val hit0 = icache_tag_0.hit.asBool && icache_tag_0.valid.asBool
    val hit1 = icache_tag_1.hit.asBool && icache_tag_1.valid.asBool

    // val stage_1_write_en = Mux(hit,)
    // val hit_L = (icache_L_tag_0.hit.asBool && icache_L_tag_0.valid.asBool) || 
    //     (icache_L_tag_1.hit.asBool && icache_L_tag_1.valid.asBool)
    // val hit_R = (icache_R_tag_0.hit.asBool && icache_R_tag_0.valid.asBool) || 
    //     (icache_R_tag_1.hit.asBool && icache_R_tag_1.valid.asBool) //只有两个指令同时未命中
    
    val stage2_sram_addr_reg = RegInit(0.U(32.W))//RegEnable(Mux(stage2_flush,0.U,stage1_sram_addr_reg),0.U,stage2_stall) //把这个加4的过程放到stage2，尽量减少stage1的逻辑层数

    stage2_sram_addr_reg :=  Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_addr_reg,stage2_sram_addr_reg))
    val stage2_sram_addr_plus_reg = RegInit(0.U(32.W))
    val stage2_sram_addr_plus_plus_reg = RegInit(0.U(32.W))
    stage2_sram_addr_plus_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_addr_reg + 4.U,stage2_sram_addr_plus_reg))
    stage2_sram_addr_plus_plus_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_addr_reg + 8.U,stage2_sram_addr_plus_plus_reg))
    // val stage2_sram_addr_plus_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_addr_reg + 4.U),0.U,stage2_stall) //后面依靠output_valid 信号来决定哪个信号是有效的
    // val stage2_sram_addr_plus_plus_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_addr_reg + 8.U),0.U,stage2_stall)

    val stage2_sram_cache_reg = RegInit(0.U.asBool)
    stage2_sram_cache_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_cache_reg,stage2_sram_cache_reg))
        //RegEnable(Mux(stage2_flush,0.U,stage1_sram_cache_reg),0.U(1.W),stage2_stall)   
    val stage2_sram_req_reg = RegInit(0.U.asBool)
    stage2_sram_req_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,stage1_sram_req_reg,Mux(io.port.sram_write_en === 0.U,stage2_sram_req_reg,0.U)))
   // val stage2_sram_req_reg = RegEnable(Mux(stage2_flush,0.U,stage1_sram_req_reg),0.U(1.W),stage2_stall)  
    
    val stage2_hit0_reg = RegInit(0.U.asBool)
    stage2_hit0_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,hit0,stage2_hit0_reg))
        
       // RegEnable(Mux(stage2_flush,0.U(1.W),hit0),0.U(1.W),stage2_stall) 
    val stage2_hit1_reg = RegInit(0.U.asBool)
    stage2_hit1_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,hit1,stage2_hit1_reg))
       // RegEnable(Mux(stage2_flush,0.U(1.W),hit1),0.U(1.W),stage2_stall) 

    val stage2_hit_reg  = RegInit(0.U.asBool)
    stage2_hit_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,hit,stage2_hit_reg))
        //RegEnable(Mux(stage2_flush,0.U,hit),0.U(1.W),stage2_stall) 
    val stage2_data_valid_reg = RegInit(0.U(3.W))

    stage2_data_valid_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,Mux(!stage1_sram_cache_reg.asBool,"b111".U,Mux(stage1_sram_addr_reg(4,2) <= 5.U , "b111".U,
        Mux(stage1_sram_addr_reg(4,2)  === 6.U,"b011".U,"b001".U))),stage2_data_valid_reg))

    val stage2_write_en_reg = RegInit(0.U(2.W))
    stage2_write_en_reg := Mux(stage2_flush,0.U,Mux(stage2_stall,Mux(stage1_sram_req_reg.asBool,Mux(access_stage1_sram_valid === 1.U,0.U,Mux(access_stage1_sram_valid === 2.U,1.U,Mux(!stage1_sram_cache_reg.asBool,3.U,Mux(stage1_sram_addr_reg(4,2) <= 5.U , 3.U,
        Mux(stage1_sram_addr_reg(4,2)  === 6.U,2.U,1.U))))),1.U),stage2_write_en_reg))
    //stage 3  存入指令缓冲队列，在issue阶段前仍然为顺序结构

   // val stage_2_work_state = RegInit(0.U(1.W))
   // val access_stage_2_work_state = Wire(Bool())
   // io.cache_working_on := access_stage_2_work_state
    //access_stage_2_work_state := Mux(stage2_sram_req_reg.asBool,1.U,stage_2_work_state)
   // stage_2_work_state := access_stage_2_work_state
    val word_L_selection0 = icache_data_way0(stage2_sram_addr_reg(4,2)).rdata
    val word_L_selection1 = icache_data_way1(stage2_sram_addr_reg(4,2)).rdata

    val word_M_selection0 = icache_data_way0(stage2_sram_addr_plus_reg(4,2)).rdata
    val word_M_selection1 = icache_data_way1(stage2_sram_addr_plus_reg(4,2)).rdata
    
    val word_R_selection0 = icache_data_way0(stage2_sram_addr_plus_plus_reg(4,2)).rdata
    val word_R_selection1 = icache_data_way1(stage2_sram_addr_plus_plus_reg(4,2)).rdata

    val hit_word_L = Mux(stage2_hit0_reg.asBool,word_L_selection0,word_L_selection1) //如果没有命中可以通过data_ok来判断是否需要接受数据
    val hit_word_M = Mux(stage2_hit0_reg.asBool,word_M_selection0,word_M_selection1) 
    val hit_word_R = Mux(stage2_hit0_reg.asBool,word_R_selection0,word_R_selection1)

    val has_stage2_stall = RegInit(0.U.asBool)
    has_stage2_stall := stage2_stall

    val sram_rdata_L_Reg = RegInit(0.U(32.W))
    val sram_rdata_M_Reg = RegInit(0.U(32.W))
    val sram_rdata_R_Reg = RegInit(0.U(32.W))

    val access_sram_rdata_L =  Mux(work_state === state_data_ready ,wait_data_L,Mux(work_state === state_lookup,hit_word_L,0.U))
    val access_sram_rdata_M =  Mux(work_state === state_data_ready ,wait_data_M,Mux(work_state === state_lookup,hit_word_M,0.U))
    val access_sram_rdata_R =  Mux(work_state === state_data_ready ,wait_data_R,Mux(work_state === state_lookup,hit_word_R,0.U))

    sram_rdata_L_Reg := Mux(has_stage2_stall,access_sram_rdata_L,sram_rdata_L_Reg)
    sram_rdata_M_Reg := Mux(has_stage2_stall,access_sram_rdata_M,sram_rdata_M_Reg)
    sram_rdata_R_Reg := Mux(has_stage2_stall,access_sram_rdata_R,sram_rdata_R_Reg)  

    
    io.port.sram_rdata_L := Mux(!io.inst_buffer_full && has_stage2_stall,access_sram_rdata_L,sram_rdata_L_Reg)
    io.port.sram_rdata_M := Mux(!io.inst_buffer_full && has_stage2_stall,access_sram_rdata_M,sram_rdata_M_Reg)
    io.port.sram_rdata_R := Mux(!io.inst_buffer_full && has_stage2_stall,access_sram_rdata_R,sram_rdata_R_Reg)

    // val write_has_stop = RegInit(0.U.asBool)
    // when(io.inst_buffer_full && has_stage2_stall) {
    //     write_has_stop := 1.U
    // }.elsewhen(!io.inst_buffer_full) {
    //     write_has_stop := 0.U
    // }.otherwise{
    //     write_has_stop := write_has_stop
    // }

    access_work_state :=  Mux(work_state===state_access_ram_0 && io.port.arready.asBool,state_access_ram_1,
        Mux(work_state===state_access_ram_1 ,Mux(io.port.rlast.asBool && io.port.rvalid.asBool,state_data_ready,state_access_ram_1),
        Mux(work_state===state_data_ready,Mux(hit.asBool.asBool,Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_lookup,Mux(stage1_finished,state_data_ready,state_access_ram_0)),state_lookup),
           Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,Mux(stage1_finished,state_data_ready,state_miss_access_ram_0),Mux(stage1_finished,state_data_ready,state_access_ram_0)),state_lookup)),
           //Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_lookup,state_access_ram_0),state_lookup),
        Mux(work_state===state_lookup,Mux(hit.asBool.asBool,Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_lookup,state_access_ram_0),state_lookup),
           Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_miss_access_ram_0,state_access_ram_0),state_lookup)),
        Mux(work_state===state_miss_access_ram_0 && io.port.arready.asBool,state_miss_access_ram_1,
        Mux(work_state===state_miss_access_ram_1 ,Mux(io.port.rlast.asBool && io.port.rvalid.asBool,state_miss_update,work_state),
        Mux(work_state===state_miss_update,state_data_ready,work_state)))))))//咱们复位都是在一个时钟周期内复位的

    // work_state := //Mux(work_state === state_reset && stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_lookup,state_access_ram_0),
    //     Mux(work_state===state_access_ram_0 && io.port.arready.asBool,state_access_ram_1,
    //     Mux(work_state===state_access_ram_1 ,Mux(io.port.rlast.asBool && io.port.rvalid.asBool,state_data_ready,state_access_ram_1),
    //     Mux(work_state===state_data_ready,Mux(stage2_sram_req_reg.asBool,Mux(stage2_sram_cache_reg.asBool,state_lookup,state_access_ram_0),state_lookup),
    //     Mux(work_state===state_lookup,Mux(stage2_hit_reg.asBool,Mux(stage2_sram_req_reg.asBool,Mux(stage2_sram_cache_reg.asBool,state_lookup,state_access_ram_0),state_lookup),
    //        Mux(stage2_sram_req_reg.asBool,Mux(stage2_sram_cache_reg.asBool,state_miss_access_ram_0,state_access_ram_0),state_lookup)),
    //     Mux(work_state===state_miss_access_ram_0 && io.port.arready.asBool,state_miss_access_ram_1,
    //     Mux(work_state===state_miss_access_ram_1 ,Mux(io.port.rlast.asBool && io.port.rvalid.asBool,state_miss_update,work_state),
    //     Mux(work_state===state_miss_update,state_data_ready,work_state)))))))//咱们复位都是在一个时钟周期内复位的
    work_state := access_work_state//Mux(work_state === state_reset && stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_lookup,state_access_ram_0),
        // Mux(work_state===state_access_ram_0 && io.port.arready.asBool,state_access_ram_1,
        // Mux(work_state===state_access_ram_1 ,Mux(io.port.rlast.asBool && io.port.rvalid.asBool,state_data_ready,state_access_ram_1),
        // Mux(work_state===state_data_ready,Mux(stage1_sram_req_reg.asBool,Mux(stage1_sram_cache_reg.asBool,state_lookup,state_access_ram_0),state_lookup),
        // Mux(work_state===state_lookup,Mux(stage2_hit_reg.asBool,Mux(stage2_sram_req_reg.asBool,Mux(stage2_sram_cache_reg.asBool,state_lookup,state_access_ram_0),state_lookup),
        //    Mux(stage2_sram_req_reg.asBool,Mux(stage2_sram_cache_reg.asBool,state_miss_access_ram_0,state_access_ram_0),state_lookup)),
        // Mux(work_state===state_miss_access_ram_0 && io.port.arready.asBool,state_miss_access_ram_1,
        // Mux(work_state===state_miss_access_ram_1 ,Mux(io.port.rlast.asBool && io.port.rvalid.asBool,state_miss_update,work_state),
        // Mux(work_state===state_miss_update,state_data_ready,work_state)))))))//咱们复位都是在一个时钟周期内复位的
    wait_data_L := Mux(work_state === state_access_ram_1 && io.port.rvalid.asBool && write_counter === 0.U,io.port.rdata,
         Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool && write_counter === stage1_sram_addr_reg(4,2),io.port.rdata,wait_data_L))

    wait_data_M := Mux(work_state === state_access_ram_1 && io.port.rvalid.asBool && write_counter === 1.U,io.port.rdata,
         Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool && write_counter === (stage1_sram_addr_reg(4,2) + 1.U(3.W)),io.port.rdata,wait_data_M))    

    wait_data_R := Mux(work_state === state_access_ram_1 && io.port.rvalid.asBool && write_counter === 2.U,io.port.rdata,
         Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool && write_counter === (stage1_sram_addr_reg(4,2) + 2.U(3.W)),io.port.rdata,wait_data_R))    

    write_counter := Mux(work_state === state_miss_access_ram_1 || work_state === state_access_ram_1,Mux(io.port.rvalid.asBool && io.port.rlast.asBool,0.U,Mux(io.port.rvalid.asBool,write_counter+1.U,write_counter)),write_counter)
    // val write_counter_same = write_counter === sram_addr_reg(4,2) && work_state === state_miss_access_ram_1 && io.port.rvalid.asBool && hit
    
    io.port.sram_data_valid := stage2_data_valid_reg
    

    for(i <- 0 to 7 ) {icache_data_way0(i).wen  := Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool
        && lru(stage1_sram_addr_reg(11,5)) === 0.U && write_counter === i.asUInt,"b1111".U,0.U) }
    for(i <- 0 to 7 ) {icache_data_way1(i).wen  := Mux(work_state === state_miss_access_ram_1 && io.port.rvalid.asBool
        && lru(stage1_sram_addr_reg(11,5)) === 1.U && write_counter === i.asUInt,"b1111".U,0.U) }
    
    icache_tag_0.wen := Mux(work_state === state_miss_update && lru(stage1_sram_addr_reg(11,5)) === 0.U,1.U,0.U)
    icache_tag_1.wen := Mux(work_state === state_miss_update  && lru(stage1_sram_addr_reg(11,5)) === 1.U,1.U,0.U)
    icache_tag_0.wdata := Mux(work_state === state_miss_update  ,Cat(1.U(1.W),stage1_sram_addr_reg(31,12)),0.U)  
    icache_tag_1.wdata := Mux(work_state === state_miss_update  ,Cat(1.U(1.W),stage1_sram_addr_reg(31,12)),0.U)

       
    io.fec_1_pc_valid := Mux(!stage1_sram_cache_reg.asBool,"b111".U,Mux(stage1_sram_addr_reg(4,2) <= 5.U , "b111".U,
        Mux(stage1_sram_addr_reg(4,2)  === 6.U,"b011".U,"b001".U)))
    //axi signal
    io.port.arid := 0.U
    io.port.araddr := Mux(work_state === state_access_ram_0,stage1_sram_addr_reg,
        Mux(work_state === state_miss_access_ram_0,Cat(stage1_sram_addr_reg(31,5),0.U(5.W)),0.U))
    io.port.arlen  := Mux(stage1_sram_cache_reg.asBool,"b111".U,"b010".U)//毕竟是双发,来点双发的样子 //一次来三个
    io.port.arsize := "b010".U
    io.port.arburst := 1.U//Mux(sram_cache_reg.asBool,1.U,0.U) //啥时候都得burst传输 如果都可以burst传输的话，不知道支持不支持burst
    io.port.arlock  := 0.U
    io.port.arcache := 0.U
    io.port.arprot  := 0.U
    io.port.arvalid := (work_state === state_access_ram_0 || work_state === state_miss_access_ram_0)
    io.port.rready  := 1.U

    io.port.sram_addr_ok := 1.U
    io.port.sram_data_ok := Mux(work_state === state_data_ready,1.U,0.U)//|write_counter_same
    io.sram_hit :=  Mux(work_state === state_lookup ,stage2_hit_reg,0.U)

    //io.port.sram_write_en := Mux(io.sram_hit.asBool || io.port.sram_data_ok.asBool,stage2_write_en_reg,0.U)

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
    io.port.sram_write_en := Mux(io.inst_buffer_full,0.U, Mux((stage2_sram_req_reg /*&& (io.port.sram_data_ok.asBool || io.sram_hit.asBool )*/) || 
        (!stage2_sram_req_reg && stage2_sram_addr_reg(1,0) =/= 0.U ) ,stage2_write_en_reg,0.U))//当出现inst error 的时候
}
object inst_cache_test extends App{
    (new ChiselStage).emitVerilog(new inst_cache)
}


