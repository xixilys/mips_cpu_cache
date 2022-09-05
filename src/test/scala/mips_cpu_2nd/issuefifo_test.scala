// package mips_cpu_2nd

// import org.scalatest.freespec.AnyFreeSpec
// import chisel3._
// import chiseltest._
// import org.scalatest.freespec.AnyFreeSpec
// import chisel3.experimental.BundleLiterals._

// import scala.collection.mutable

// //首先先测试输入输出均为2的时候的状态
// class issuefifo_test  extends AnyFreeSpec with ChiselScalatestTester {
//   var index = 0
//   var Randlist = List(0.5507575307052524, 0.856706258793442, 0.5841163107479006, 0.5177410741717722, 0.9810326665138783, 0.5779652138674927, 0.7067479821892834, 0.21595208456197335, 0.7931859268885686, 0.42040429388979106, 0.17661542019557785, 0.12798169322751152, 0.267492436541655, 0.9296620261971654, 0.9100669428484961, 0.8605144917159215, 0.31900313030122607, 0.7712306450277208, 0.8560511166540524, 0.42038580113609514, 0.06239508150353801, 0.9282994036801363, 0.7317209659737166, 0.03500321239100712, 0.39471374663566217, 0.6290604211706909, 0.8963983172572256, 0.9180354780529424, 0.651640473965878, 0.03689409784341324, 0.13876987945230612, 0.7134932837918259, 0.5735831512020514, 0.1909518772812312, 0.942226135951068, 0.03980147795423572, 0.4673017253656858, 0.8575069362409946, 0.552042876108001, 0.46269015786532874, 0.5669437554801031, 0.8440436308327898, 0.8956444923290482, 0.35170792172362075, 0.18528068854268243, 0.9384089543533194, 0.10347699870617533, 0.4198873373929929, 0.3015977724617631, 0.9644506216111465, 0.04305798322687826, 0.30956906187455524, 0.7571062758216786, 0.49341777403305564, 0.9125597022697635, 0.3696181108443627, 0.2219096554020884, 0.5170021165481951, 0.5428205018242591, 0.7702415892204951, 0.559469566337327, 0.8732484485240898, 0.6279690920748815, 0.32134458249531517)

//   def  Myrandom():Double=
//   {
//     var res=0.0
// //    res = Randlist(index)
// //    index = index + 1
// //    if(index > Randlist.size-1)
// //      index = 0
// //
//     res = Math.random()
//     res

//   }
//   "DUt pass is lys shabi" in {
//     test(new issue_queue(16)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

//       var simuque: List[Int] = List()
//       var expectpop: List[Int] = List()
//       for (i <- 0 until 3000) {
//         expectpop = List()
//         var writenum = (Myrandom() * 3).toInt
//         var readnum = (Myrandom() * 3).toInt
//         if(writenum ==3) writenum =  2
//         if(readnum ==3) readnum =  2
// //          var writenum = 1
// //          var readnum = 1
//         if(simuque.size ==14)
//           {
//             writenum = 2
//           }
//         else if(simuque.size == 15) {
//           writenum  = 1
//         }
//         else if (simuque.size == 16) {
//           if(readnum == 0) {
//             writenum = 0
//           }else if(readnum == 1) {
//             if(writenum > 1) {
//               writenum = 1
//             }
//             else {
//               writenum = 0
//             }
//           }


//         }




//         if(simuque.size < 14)
//             {
//               readnum = Math.max(0,readnum-1)
//             }
//         println("============" + i)
//         println(simuque)
//         println("Size  "+simuque.size)
//         println("writenum  : " + writenum)



// //        println("quesize : " + simuque.size)

//         if(readnum > simuque.size)
//           readnum = simuque.size
//         println("readnum : " + readnum)
//         /**********************************/
//         dut.io.inst_issue_num.poke(readnum.U)
//         dut.io.inst_write.poke(writenum.U)
//         dut.io.inst_issue_addr(0).poke(0.U)
//         dut.io.inst_issue_addr(1).poke(0.U)
//         /**********************************/
// //        println("readnum : " + readnum)
//         if (readnum <= simuque.size) {
//           var addrqueue: List[Int] = List()
//           var preaddr = (Myrandom() * simuque.size).toInt
//           while (addrqueue.size < readnum) {
//             while (addrqueue.exists(s => s == preaddr.toInt)) {
//               preaddr = (Myrandom() * simuque.size).toInt
//             }
//             addrqueue = preaddr :: addrqueue
//           }
         
//           addrqueue = addrqueue.sorted.reverse

//           println("addr que pop " + addrqueue)

//           for (j <- 0 until readnum) {

//             var addr = addrqueue.apply(j)
//             /**********************************/
//             dut.io.inst_issue_addr(j).poke((simuque.size - addr -1 ).U)
// //            println("Actual POP" + dut.io_out(j).opcode.peek().toString)
//             /**********************************/
//             expectpop = simuque.apply(addr) :: expectpop

// //            println("POP val" + simuque.apply(addr))
//             println("POP addr" + (simuque.size - addr -1 ))
//           }
//           // println("expect " +expectpop)
//           expectpop= expectpop.reverse
//           // println("expect " +expectpop)
//           var tmp: List[Int] = List()
//           for (j <- 0 until simuque.size) {

//             if (addrqueue.exists(s => s == j) == false) {
//               tmp = simuque.apply(j).toInt :: tmp
//             }
//           }
//           simuque = tmp.reverse
// //          println("after pop " + simuque)
//         }
//         for (j <- 0 until 2) {
//           if (j < writenum) {
//             var in = (Myrandom() * 127).toInt +1
//             simuque = in.toInt :: simuque
//             /**********************************/
//             dut.io_in(j).psc_src1.poke(0.U)
//             dut.io_in(j).psc_src2.poke(0.U)
//             dut.io_in(j).psc_dest.poke(0.U)
//             dut.io_in(j).ready.poke(0.U.asBool)
//             dut.io_in(j).opcode.poke(in.U)
//             /**********************************/
//             println("PUSH sim" + in)
//           }
//           else
//             {
//               dut.io_in(j).psc_src1.poke(0.U)
//               dut.io_in(j).psc_src2.poke(0.U)
//               dut.io_in(j).psc_dest.poke(0.U)
//               dut.io_in(j).ready.poke(0.U.asBool)
//               dut.io_in(j).opcode.poke(0.U)
//             }
//           //
//         }
//         for (j <- 0 until readnum) {

//           //          var addr = addrqueue.apply(j)
//           /**********************************/
//           println("Expect POP " + expectpop(j).U)
//           println("Actual POP " + dut.io_out(j).opcode.peek().toString)
//           dut.io_out(j).opcode.expect(expectpop(j).U)
//           /**********************************/
//         }
//         dut.clock.step()
//       }
//     }

//   }
// }
