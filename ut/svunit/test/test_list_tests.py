import re
import subprocess

import pytest

from utils import *


def test_list_tests_option_exists(tmp_path):
    returncode = subprocess.call(
            ['runSVUnit', '--list-tests'],
            cwd=tmp_path)
    assert returncode == 255  # XXX Fix reliance on internal implementation detail: if the script can't run, it quietly returns `255`


@pytest.fixture(scope="function")
def setup_with_one_test_case(tmp_path, monkeypatch):
    some_unit_test = tmp_path.joinpath('some_unit_test.sv')
    some_unit_test.write_text('''
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
      `FAIL_IF(1)
    `SVTEST_END

  `SVUNIT_TESTS_END

endmodule
    ''')

    monkeypatch.chdir(tmp_path)


@all_available_simulators()
def test_that_tests_are_not_run_when_option_used(simulator, setup_with_one_test_case):
    subprocess.check_call(['runSVUnit', '-s', simulator, '--list-tests'])
    log = pathlib.Path('run.log')
    assert 'FAILED' not in log.read_text()


@all_available_simulators()
def test_that_test_cases_are_not_run_when_option_used(simulator, setup_with_one_test_case):
    subprocess.check_call(['runSVUnit', '-s', simulator, '--list-tests'])

    with open(pathlib.Path('run.log'), 'r') as log:
        test_case_running = re.compile("some_ut.*RUNNING")
        assert not any(test_case_running.search(line) for line in log)


@all_available_simulators()
def test_that_test_suites_are_not_run_when_option_used(simulator, setup_with_one_test_case):
    subprocess.check_call(['runSVUnit', '-s', simulator, '--list-tests'])

    with open(pathlib.Path('run.log'), 'r') as log:
        test_suite_running = re.compile("_ts.*RUNNING")
        assert not any(test_suite_running.search(line) for line in log)


@all_available_simulators()
def test_that_nothing_is_run_when_option_used(simulator, setup_with_one_test_case):
    subprocess.check_call(['runSVUnit', '-s', simulator, '--list-tests'])
    log = pathlib.Path('run.log')
    assert 'RUNNING' not in log.read_text()


@all_available_simulators()
def test_that_status_is_not_reported_when_option_used(simulator, setup_with_one_test_case):
    subprocess.check_call(['runSVUnit', '-s', simulator, '--list-tests'])
    log = pathlib.Path('run.log')
    assert 'PASSED' not in log.read_text()


@all_available_simulators()
def test_that_test_from_test_case_is_printed_when_option_used(simulator, setup_with_one_test_case):
    subprocess.check_call(['runSVUnit', '-s', simulator, '--list-tests'])
    with open(pathlib.Path('run.log'), 'r') as log:
        if simulator not in ['modelsim', 'qrun']:
            assert sum(1 for line in log if line == "    some_test\n") == 1
        else:
            assert sum(1 for line in log if line == "#     some_test\n") == 1


@all_available_simulators()
def test_that_test_case_is_printed_when_option_used(simulator, setup_with_one_test_case):
    subprocess.check_call(['runSVUnit', '-s', simulator, '--list-tests'])
    log = pathlib.Path('run.log')
    if simulator not in ['modelsim', 'qrun']:
        assert "some_ut\n    some_test\n" in log.read_text()
    else:
        assert "# some_ut\n#     some_test\n" in log.read_text()
