test:
	mill emulator[examples.Adder,SimpleGenerator].mfccompiler.compile

hello:
	mill -i changbaiV1.spinal.runMain genChangbai

play:
	mill -i changbaiV1.spinal.test.runMain changbaiV1.v1.test.Play

gshare:
	mill -i changbaiV1.spinal.runMain v1.prediction.GenGshare

csr:
	mill -i changbaiV1.spinal.runMain v1.csr.GenCSR

regfile:
	mill -i changbaiV1.spinal.runMain v1.regfile.GenRegfile

alu:
	mill -i changbaiV1.spinal.runMain v1.alu.GenAlu

rw64fetch:
	mill -i changbaiV1.spinal.runMain v1.rw64fetch.GenRw64Fetch

rvc_decoder:
	mill -i changbaiV1.spinal.runMain v1.rvc.GenRVCDecoder

rvc_expander:
	mill -i changbaiV1.spinal.runMain v1.rvc.GenRVCExpander

inst_queue:
	mill -i changbaiV1.spinal.runMain v1.rvc.GenInstQueue

frontend:
	mill -i changbaiV1.spinal.runMain v1.GenFrontend

harness:
	mill -i changbaiV1.spinal.runMain v1.GenTestHarness

harness_bootrom:
	mill -i changbaiV1.spinal.runMain v1.GenTestHarness env/coco_tb/TestRam/bootrom/bootrom.img

scalar_decode:
	mill -i changbaiV1.spinal.runMain v1.GenScalarDecode

integration:
	mill -i changbaiV1.spinal.runMain v1.integration.GenIntegrationTop

testram:
	mill -i changbaiV1.spinal.runMain v1.testram.GenTestRam

testram_bootrom:
	mill -i changbaiV1.spinal.runMain v1.testram.GenTestRam 8 2048 env/coco_tb/TestRam/bootrom/bootrom.img

signext:
	mill -i changbaiV1.spinal.runMain v1.GenSignExt

idea:
	mill mill.idea.GenIdea/idea

clean:
	rm -rf out/ rtl/ *.lst changbaiTest/