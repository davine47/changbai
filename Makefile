test:
	mill emulator[examples.Adder,SimpleGenerator].mfccompiler.compile

hello:
	mill -i changbaiV1.spinal.runMain genChangbai

rtl:
	mill -i changbaiV1.spinal.test.runMain changbaiV1.v1.test.Play

play:
	mill -i changbaiV1.spinal.test.runMain play.sayHello

idea:
	mill mill.idea.GenIdea/idea

clean:
	rm -rf out/ rtl/ *.lst changbaiTest/