//###########################################################################
//
//  Copyright 2011-2023 The SVUnit Authors.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//###########################################################################

/*
  Class: svunit_testsuite
  Base class for the unit test suite
*/
class svunit_testsuite extends svunit_base;

  /*
    Array: list_of_testcases
    Queue list of Unit Testcases to include for this Test Suite
  */
  protected svunit_testcase list_of_testcases[$];


  /*
    Interface
  */
  extern function new(string name);
  extern function void add_testcase(svunit_testcase svunit);
  extern task run();

  extern function void report();


  local function int unsigned get_num_passing_testcases();
    int unsigned result;
    foreach (list_of_testcases[i])
      if (list_of_testcases[i].get_results() == PASS)
        result++;
    return result;
  endfunction


  function junit_xml::TestSuite as_junit_test_suite();
    junit_xml::TestSuite result = new(get_name());
    foreach (list_of_testcases[i]) begin
      junit_xml::TestCase junit_test_cases[] = list_of_testcases[i].as_junit_test_cases();
      foreach (junit_test_cases[i])
        result.add_test_case(junit_test_cases[i]);
    end
    return result;
  endfunction

endclass


/*
  Constructor: new
  Initializes the test suite

  Parameters:
    name - instance name of the unit test suite
*/
function svunit_testsuite::new(string name);
  super.new(name);
endfunction


/*
  Method: add_testcase
  Adds a testcase to list of tests

  Parameters:
    svunit - unit test to add to the list of unit tests
*/
function void svunit_testsuite::add_testcase(svunit_testcase svunit);
  `INFO($sformatf("Registering Unit Test Case %s", svunit.get_name()));
  list_of_testcases.push_back(svunit);
endfunction


/*
  Method: run
  Main Run Task of the Test Suite
*/
task svunit_testsuite::run();
  if ($test$plusargs("SVUNIT_LIST_TESTS"))
    return;

  `INFO("RUNNING");
endtask


/*
  Method: report
  This task reports the results for the unit tests
*/
function void svunit_testsuite::report();
  int     pass_cnt;
  string  success_str;

  if ($test$plusargs("SVUNIT_LIST_TESTS"))
    return;

  foreach(list_of_testcases[i])
    list_of_testcases[i].report();

  pass_cnt = get_num_passing_testcases();

  if (pass_cnt == list_of_testcases.size()) begin
    success_str = "PASSED";
    success = PASS;
  end else begin
    success_str = "FAILED";
    success = FAIL;
  end

  `LF;
  `INFO($sformatf("%0s (%0d of %0d testcases passing)",
    success_str,
    pass_cnt,
    list_of_testcases.size()));
endfunction
