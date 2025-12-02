test:
	mill emulator[examples.Adder,SimpleGenerator].mfccompiler.compile

hello:
	mill -i changbaiV1.spinal.runMain changbaiV1.sayHello

rtl:
	mill -i changbaiV1.spinal.test.runMain changbaiV1.v1.test.Play

idea:
	mill mill.idea.GenIdea/idea

clean:
	rm -rf out/ *.lst changbaiTest/