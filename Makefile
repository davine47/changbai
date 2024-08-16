test:
	mill emulator[sandbox.Hello,sandbox.HelloConfig,DiplomacyGenerator].mfccompiler.compile
test1:
	mill emulator[examples.Adder,sandbox.HelloConfig,SimpleGenerator].mfccompiler.compile
v1-adder:
	mill -i changbaiV1.spinal.runMain v1.AdderExample

v1-regfile:
	mill -i changbaiV1.spinal.runMain v1.RegFileExample

v1-decoder:
	mill -i changbaiV1.spinal.test.runMain changbaiV1.v1.test.IDecoder

v1-alu:
	mill -i changbaiV1.spinal.test.runMain changbaiV1.v1.test.ALU

v1-print:
	mill -i changbaiV1.spinal.test.runMain changbaiV1.v1.test.Print

v1-play:
	mill -i changbaiV1.spinal.test.runMain changbaiV1.v1.test.Play

hello:
	mill -i changbaiV1.spinal.runMain changbaiV1.sayHello

changbai:
	mill -i changbaiV1.spinal.runMain changbaiV1.genChangbai

idea:
	mill mill.idea.GenIdea/idea

clean:
	rm -rf out/ *.lst changbaiTest/