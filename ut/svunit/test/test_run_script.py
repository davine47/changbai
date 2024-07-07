import subprocess

import pytest

from utils import *


@all_available_simulators()
def test_filter(tmp_path, simulator):
    unit_test = tmp_path.joinpath('some_unit_test.sv')
    unit_test.write_text('''
module some_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_failing_test)
      `FAIL_IF(1)
    `SVTEST_END

    `SVTEST(some_passing_test)
      `FAIL_IF(0)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    log = tmp_path.joinpath('run.log')

    print('Filtering only the passing test should block the fail')
    subprocess.check_call(['runSVUnit', '-s', simulator, '--filter', 'some_ut.some_passing_test'], cwd=tmp_path)
    assert 'FAILED' not in log.read_text()

    print('No explicit filter should cause both tests to run, hence trigger the fail')
    subprocess.check_call(['runSVUnit', '-s', simulator], cwd=tmp_path)
    assert 'FAILED' in log.read_text()


@all_available_simulators()
def test_filter_wildcards(tmp_path, simulator):
    failing_unit_test = tmp_path.joinpath('some_failing_unit_test.sv')
    failing_unit_test.write_text('''
module some_failing_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_failing_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_test)
      `FAIL_IF(1)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    passing_unit_test = tmp_path.joinpath('some_passing_unit_test.sv')
    passing_unit_test.write_text('''
module some_passing_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_passing_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_test)
      `FAIL_IF(0)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')
    log = tmp_path.joinpath('run.log')

    print('Filtering only the passing testcase should block the fail')
    subprocess.check_call(['runSVUnit', '-s', simulator, '--filter', 'some_passing_ut.*'], cwd=tmp_path)
    assert 'FAILED' not in log.read_text()
    assert 'some_test' in log.read_text()

    print('Filtering only for the test should cause both tests to run, hence trigger the fail')
    subprocess.check_call(['runSVUnit', '-s', simulator, '--filter', "*.some_test"], cwd=tmp_path)
    assert 'FAILED' in log.read_text()


@all_available_simulators()
def test_filter_without_dot(tmp_path, simulator):
    dummy_unit_test = tmp_path.joinpath('dummy_unit_test.sv')
    dummy_unit_test.write_text('''
module dummy_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_passing_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN
  `SVUNIT_TESTS_END

endmodule
    ''')

    subprocess.call(['runSVUnit', '-s', simulator, '--filter', 'some_string'], cwd=tmp_path)

    log = tmp_path.joinpath('run.log')
    assert 'fatal' in log.read_text().lower()


@all_available_simulators()
def test_filter_with_extra_dot(tmp_path, simulator):
    dummy_unit_test = tmp_path.joinpath('dummy_unit_test.sv')
    dummy_unit_test.write_text('''
module dummy_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_passing_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN
  `SVUNIT_TESTS_END

endmodule
    ''')

    subprocess.call(['runSVUnit', '-s', simulator, '--filter', 'a.b.c'], cwd=tmp_path)

    log = tmp_path.joinpath('run.log')
    assert 'fatal' in log.read_text().lower()


@all_available_simulators()
def test_filter_with_partial_widlcard(tmp_path, simulator):
    dummy_unit_test = tmp_path.joinpath('dummy_unit_test.sv')
    dummy_unit_test.write_text('''
module dummy_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_passing_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN
  `SVUNIT_TESTS_END

endmodule
    ''')

    subprocess.call(['runSVUnit', '-s', simulator, '--filter', 'foo*.bar'], cwd=tmp_path)
    log = tmp_path.joinpath('run.log')
    assert 'fatal' in log.read_text().lower()

    subprocess.call(['runSVUnit', '-s', simulator, '--filter', 'foo.bar*'], cwd=tmp_path)
    log = tmp_path.joinpath('run.log')
    assert 'fatal' in log.read_text().lower()

    subprocess.call(['runSVUnit', '-s', simulator, '--filter', '*foo.bar'], cwd=tmp_path)
    log = tmp_path.joinpath('run.log')
    assert 'fatal' in log.read_text().lower()


@all_available_simulators()
def test_multiple_filter_expressions(tmp_path, simulator):
    unit_test = tmp_path.joinpath('some_unit_test.sv')
    unit_test.write_text('''
module some_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_failing_test)
      `FAIL_IF(1)
    `SVTEST_END

    `SVTEST(some_passing_test)
      `FAIL_IF(0)
    `SVTEST_END

    `SVTEST(some_other_passing_test)
      `FAIL_IF(0)
    `SVTEST_END

    `SVTEST(yet_another_passing_test)
      `FAIL_IF(0)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    log = tmp_path.joinpath('run.log')

    print('Filtering only the passing testcases should block the fail')
    subprocess.check_call(
          [
              'runSVUnit',
              '-s', simulator,
              '--filter', '*.some_passing_test:*.some_other_passing_test:*.yet_another_passing_test',
              ],
          cwd=tmp_path)
    assert 'FAILED' not in log.read_text()
    assert 'some_passing_test' in log.read_text()
    assert 'some_other_passing_test' in log.read_text()
    assert 'yet_another_passing_test' in log.read_text()


@all_available_simulators()
def test_negative_filter(tmp_path, simulator):
    unit_test = tmp_path.joinpath('some_unit_test.sv')
    unit_test.write_text('''
module some_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_failing_test)
      `FAIL_IF(1)
    `SVTEST_END

    `SVTEST(some_other_failing_test)
      `FAIL_IF(1)
    `SVTEST_END

    `SVTEST(some_passing_test)
      `FAIL_IF(0)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    log = tmp_path.joinpath('run.log')

    print('Filtering out the failing tests should block the fail')
    subprocess.check_call(
            ['runSVUnit',
                    '-s', simulator,
                    '--filter', '-some_ut.some_failing_test:some_ut.some_other_failing_test',
                    ],
            cwd=tmp_path)
    assert 'FAILED' not in log.read_text()
    assert 'some_passing_test' in log.read_text()


@all_available_simulators()
def test_positive_and_negative_filter(tmp_path, simulator):
    unit_test = tmp_path.joinpath('some_unit_test.sv')
    unit_test.write_text('''
module some_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_failing_test)
      `FAIL_IF(1)
    `SVTEST_END

    `SVTEST(some_passing_test)
      `FAIL_IF(0)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    other_unit_test = tmp_path.joinpath('some_other_unit_test.sv')
    other_unit_test.write_text('''
module some_other_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_other_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_other_failing_test)
      `FAIL_IF(1)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    log = tmp_path.joinpath('run.log')

    print('Filtering only tests from the first unit test'
            + ' and then filtering out the failing test should block the fail')
    subprocess.check_call(
            ['runSVUnit',
                    '-s', simulator,
                    '--filter', 'some_ut.*-some_ut.some_failing_test',
                    ],
            cwd=tmp_path)
    assert 'FAILED' not in log.read_text()
    assert 'some_passing_test' in log.read_text()


@all_available_simulators()
def test_nothing_printed_for_module_where_no_tests_selected(tmp_path, simulator):
    unit_test = tmp_path.joinpath('some_unit_test.sv')
    unit_test.write_text('''
module some_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_test)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    other_unit_test = tmp_path.joinpath('some_other_unit_test.sv')
    other_unit_test.write_text('''
module some_other_unit_test;

  import svunit_pkg::*;
  `include "svunit_defines.svh"

  string name = "some_other_ut";
  svunit_testcase svunit_ut;

  function void build();
    svunit_ut = new(name);
  endfunction

  task setup();
    svunit_ut.setup();
  endtask

  task teardown();
    svunit_ut.teardown();
  endtask

  `SVUNIT_TESTS_BEGIN

    `SVTEST(some_other_test)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    print('Nothing should be printed for some_ut')
    subprocess.check_call(
            ['runSVUnit',
                    '-s', simulator,
                    '--filter', 'some_other_ut.*',
                    ],
            cwd=tmp_path)

    with open(pathlib.Path(tmp_path.joinpath('run.log')), 'r') as log:
        test_case_running = re.compile("some_ut.*RUNNING")
        assert not any(test_case_running.search(line) for line in log)

    with open(pathlib.Path(tmp_path.joinpath('run.log')), 'r') as log:
        test_case_running = re.compile("some_ut.*PASSED")
        assert not any(test_case_running.search(line) for line in log)


def test_verilator_does_not_accept_uvm(tmp_path):
    cmdline_usage_error = 4
    returncode = subprocess.call(
            ['runSVUnit', '--sim', 'verilator', '--uvm'],
            cwd=tmp_path)
    assert returncode == cmdline_usage_error


def test_verilator_does_not_accept_mixedsim(tmp_path):
    cmdline_usage_error = 4
    returncode = subprocess.call(
            ['runSVUnit', '--sim', 'verilator', '--mixedsim', 'dummy'],
            cwd=tmp_path)
    assert returncode == cmdline_usage_error


def test_non_zero_exit_code_from_executed_command_signals_internal_execution_error(tmpdir, monkeypatch):
    internal_execution_error = 3

    with tmpdir.as_cwd():
        some_unit_test_that_gets_us_over_test_collection = pathlib.Path.cwd().joinpath('some_unit_test.sv')
        some_unit_test_that_gets_us_over_test_collection.write_text("dummy content")

        FakeTool.that_fails('xrun')
        monkeypatch.setenv('PATH', '.', prepend=os.pathsep)

        returncode = subprocess.call(['runSVUnit', '--sim', 'xcelium'])

    assert returncode == internal_execution_error


def test_non_zero_exit_code_from_vlog_signals_internal_execution_error(tmpdir, monkeypatch):
    internal_execution_error = 3

    with tmpdir.as_cwd():
        some_unit_test_that_gets_us_over_test_collection = pathlib.Path.cwd().joinpath('some_unit_test.sv')
        some_unit_test_that_gets_us_over_test_collection.write_text("dummy content")

        FakeTool.that_succeeds('vlib')
        FakeTool.that_fails('vlog')
        FakeTool.that_succeeds('vsim')
        monkeypatch.setenv('PATH', '.', prepend=os.pathsep)

        returncode = subprocess.call(['runSVUnit', '--sim', 'questa'])

    assert returncode == internal_execution_error


def test_non_zero_exit_code_from_vlib_signals_internal_execution_error(tmpdir, monkeypatch):
    internal_execution_error = 3

    with tmpdir.as_cwd():
        some_unit_test_that_gets_us_over_test_collection = pathlib.Path.cwd().joinpath('some_unit_test.sv')
        some_unit_test_that_gets_us_over_test_collection.write_text("dummy content")

        FakeTool.that_fails('vlib')
        FakeTool.that_succeeds('vlog')
        FakeTool.that_succeeds('vsim')
        monkeypatch.setenv('PATH', '.', prepend=os.pathsep)

        returncode = subprocess.call(['runSVUnit', '--sim', 'questa'])

    assert returncode == internal_execution_error


def test_non_zero_exit_code_from_vcom_signals_internal_execution_error(tmpdir, monkeypatch):
    internal_execution_error = 3

    with tmpdir.as_cwd():
        some_unit_test_that_gets_us_over_test_collection = pathlib.Path.cwd().joinpath('some_unit_test.sv')
        some_unit_test_that_gets_us_over_test_collection.write_text("dummy content")

        FakeTool.that_succeeds('vlib')
        FakeTool.that_fails('vcom')
        FakeTool.that_succeeds('vlog')
        FakeTool.that_succeeds('vsim')
        monkeypatch.setenv('PATH', '.', prepend=os.pathsep)

        returncode = subprocess.call(['runSVUnit', '--sim', 'questa', '-mixedsim', 'dummy'])

    assert returncode == internal_execution_error


def test_compile_error_from_verilator_signals_internal_execution_error(tmpdir, monkeypatch):
    internal_execution_error = 3

    with tmpdir.as_cwd():
        some_unit_test_that_gets_us_over_test_collection = pathlib.Path.cwd().joinpath('some_unit_test.sv')
        some_unit_test_that_gets_us_over_test_collection.write_text("dummy content")

        FakeTool.that_fails('verilator')
        monkeypatch.setenv('PATH', '.', prepend=os.pathsep)

        returncode = subprocess.call(['runSVUnit', '--sim', 'verilator'])

    assert returncode == internal_execution_error
