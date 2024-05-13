test:
	mill emulator[sandbox.Hello,sandbox.HelloConfig].mfccompiler.compile

v1-adder:
	mill -i changbai.spinal.runMain v1.AdderExample

v1-regfile:
	mill -i changbai.spinal.runMain v1.RegFileExample

v1-decoder:
	mill -i changbai.spinal.test.runMain changbai.v1.test.IDecoder

hello:
	mill -i changbai.spinal.runMain changbai.sayHello

changbai:
	mill -i changbai.spinal.runMain changbai.genChangbai

idea:
	mill mill.idea.GenIdea/idea