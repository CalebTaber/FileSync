#!/bin/bash

# Usage
# test.sh [test_num]
# If no arguments are supplied, all stored tests will be run

# To create a test:
# - Add a unique branch to the if-elif statement in set_up_test()
# - Run test.sh -t <testnum>
# - Enter 'n' when prompted to grade test
# - Copy output lines before list of trashed files
# - Paste this output into tests/expected_<testnum>
# 
# Now you can run test.sh -t <testnum> OR test.sh (for all tests) and see the grading results

user_home=$(realpath ~)
setup_dir="$user_home/Desktop/filesync_test/"

expected_outputs="$user_home/Code/Bash/filesync/tests"

total_tests=$(find "$expected_outputs" -mindepth 1 -print | grep "expected_" | wc -l)
tests_passed=0


set_last_sync () {
  # Set new last_sync
  echo $(date +%s) > "$setup_dir/local/.last_sync"

  # Preserve timestamps with -p when copying last_sync to remote
  cp -p "$setup_dir/local/.last_sync" "$setup_dir/remote/"
  
  # Sleep for a short time so no files have the same ctime as the last_sync
  sleep 0.005
}


generate_basic_structure () {
  mkdir -p "$setup_dir/local/sync"
  mkdir -p "$setup_dir/remote/sync"
}


# $1 is the number of the test to set up
set_up_test () {
  # Clear and re-create local and remote
  rm -r "$setup_dir"
  generate_basic_structure
  
  if [[ $1 == "01" ]]; then
    # No changes should be made
    echo "local oldfile1" > "$setup_dir/local/sync/oldfile1"
    echo "remote oldfile1" > "$setup_dir/remote/sync/oldfile1"
    
    set_last_sync
    
  elif [[ $1 == "02" ]]; then
    # local newfile1 should be copied to remote
    set_last_sync
    echo "local newfile1" > "$setup_dir/local/sync/newfile1"
  
  elif [[ $1 == "03" ]]; then
    # remote newfile1 should be copied to local
    set_last_sync
    echo "remote newfile1" > "$setup_dir/remote/sync/newfile1"
    
  elif [[ $1 == "04" ]]; then
    # local newdir1 should be copied to remote
    set_last_sync
    mkdir "$setup_dir/local/sync/newdir"

  elif [[ $1 == "05" ]]; then
    # remote newdir1 should be copied to local
    set_last_sync
    mkdir "$setup_dir/remote/sync/newdir"
  
  elif [[ $1 == "06" ]]; then
    # local newdir1 should be copied to remote, along with its contents
    set_last_sync
    mkdir "$setup_dir/local/sync/newdir"
    echo "local newfile1" > "$setup_dir/local/sync/newdir/newfile1"
    mkdir "$setup_dir/local/sync/newdir/newdir2"
  
  elif [[ $1 == "07" ]]; then
    # remote newdir1 should be copied to local, along with its contents
    set_last_sync
    mkdir "$setup_dir/remote/sync/newdir"
    echo "remote newfile1" > "$setup_dir/remote/sync/newdir/newfile1"
    mkdir "$setup_dir/remote/sync/newdir/newdir2"
    
  elif [[ $1 == "08" ]]; then
    # remote oldfile1 should be deleted
    # remote olddir1 should be copied to local
    # remote newfile1 and newdir1 should be copied to local
    mkdir "$setup_dir/remote/sync/olddir1"
    echo "remote oldfile1" > "$setup_dir/remote/sync/olddir1/oldfile1"

    set_last_sync

    echo "remote newfile2" > "$setup_dir/remote/sync/olddir1/newfile1"
    mkdir "$setup_dir/remote/sync/olddir1/newdir1"
    
  elif [[ $1 == "09" ]]; then
    # local oldfile1 should be deleted
    # local olddir1 should be copied to remote
    # local newfile1 and newdir1 should be copied to remote
    mkdir "$setup_dir/local/sync/olddir1"
    echo "local oldfile1" > "$setup_dir/local/sync/olddir1/oldfile1"

    set_last_sync

    echo "local newfile2" > "$setup_dir/local/sync/olddir1/newfile1"
    mkdir "$setup_dir/local/sync/olddir1/newdir1"
  
  elif [[ $1 == "10" ]]; then
    # local oldfile1 should be deleted
    # local olddir1 should be deleted
    # local oldfile2 should be deleted
    mkdir "$setup_dir/local/sync/olddir1"
    echo "local oldfile1" > "$setup_dir/local/sync/oldfile1"
    echo "local oldfile2" > "$setup_dir/local/sync/olddir1/oldfile2"
    
    set_last_sync
    
  elif [[ $1 == "11" ]]; then
    # remote oldfile1 should be deleted
    # remote olddir1 should be deleted
    # remote oldfile2 should be deleted
    mkdir "$setup_dir/remote/sync/olddir1"
    echo "remote oldfile1" > "$setup_dir/remote/sync/oldfile1"
    echo "remote oldfile2" > "$setup_dir/remote/sync/olddir1/oldfile2"
    
    set_last_sync
    
  elif [[ $1 == "12" ]]; then
    # local commonfile1 should be replaced with remote
    # remote commonfile2 should be replaced with local
    # remote newdir1 and its contents should be copied to local
    # local newdir2 and its contents should be copied to remote
    # local trashfile1 should be deleted
    # remote trashfile2 should be deleted
    mkdir "$setup_dir/local/sync/olddir1"
    echo "local oldfile1" > "$setup_dir/local/sync/oldfile1"
    echo "local oldfile1" > "$setup_dir/local/sync/olddir1/oldfile1"
    echo "local trashfile1" > "$setup_dir/local/sync/olddir1/trashfile1"
    
    
    mkdir "$setup_dir/remote/sync/olddir1"
    echo "remote oldfile1" > "$setup_dir/remote/sync/oldfile1"
    echo "remote trashfile2" > "$setup_dir/remote/sync/trashfile2"
    echo "remote oldfile1" > "$setup_dir/remote/sync/olddir1/oldfile1"
    
    set_last_sync
    
    echo "remote commonfile2" > "$setup_dir/remote/sync/commonfile2"
    echo "local commonfile1" > "$setup_dir/local/sync/commonfile1"
    sleep 0.01
    echo "remote commonfile1" > "$setup_dir/remote/sync/commonfile1"
    echo "local commonfile2" > "$setup_dir/local/sync/commonfile2"
    
    mkdir "$setup_dir/remote/sync/newdir1"
    echo "remote newfile1" > "$setup_dir/remote/sync/newdir1/newfile1"
    
    mkdir "$setup_dir/local/sync/newdir2"
    echo "local newfile2" > "$setup_dir/local/sync/newdir2/newfile2"
    
  elif [[ $1 == "13" ]]; then
    # nothing should happen since /sync/newdir is in the exclude file
    set_last_sync
    
    mkdir "$setup_dir/local/sync/newdir"
    echo "local newfile1" > "$setup_dir/local/sync/newdir/newfile1"
    
    echo "/sync/newdir" > "$setup_dir/local/.sync_exclude"
  
  elif [[ $1 == "14" ]]; then
    # nothing should happen since /sync/newdir is in the exclude file
    set_last_sync
    
    mkdir "$setup_dir/remote/sync/newdir"
    echo "remote newfile1" > "$setup_dir/remote/sync/newdir/newfile1"
    
    echo "/sync/newdir" > "$setup_dir/remote/.sync_exclude"
  
  elif [[ $1 == "15" ]]; then
    # nothing should happen since /sync/newdir and /sync/newfile1 are in the remote exclude file
    # the remote exclude file should be copied over the local exclude file, thus excluding both
    # newdir1 and newfile1
    set_last_sync
    
    echo "remote newfile1" > "$setup_dir/remote/sync/newfile1"
    mkdir "$setup_dir/remote/sync/newdir"
    echo "remote newfile2" > "$setup_dir/remote/sync/newdir/newfile2"
    
    echo "/sync/newdir" > "$setup_dir/local/.sync_exclude"
    sleep 0.01
    echo "/sync/newdir" > "$setup_dir/remote/.sync_exclude"
    echo "/sync/newfile1" >> "$setup_dir/remote/.sync_exclude"
  
  elif [[ $1 == "16" ]]; then
    # nothing should happen since /sync/newdir and /sync/newfile1 are in the local exclude file
    set_last_sync
    
    echo "local newfile1" > "$setup_dir/local/sync/newfile1"
    mkdir "$setup_dir/local/sync/newdir"
    echo "local newfile2" > "$setup_dir/local/sync/newdir/newfile2"
    
    echo "/sync/newdir" > "$setup_dir/remote/.sync_exclude"
    sleep 0.01
    echo "/sync/newdir" > "$setup_dir/local/.sync_exclude"
    echo "/sync/newfile1" >> "$setup_dir/local/.sync_exclude"
  
  elif [[ $1 == "17" ]]; then
    # nothing should happen since /sync/newdir and /sync/newfile1 are in the local exclude file
    set_last_sync
    
    echo "local newfile1" > "$setup_dir/local/sync/newfile1"
    mkdir "$setup_dir/local/sync/newdir"
    echo "local newfile2" > "$setup_dir/local/sync/newdir/newfile2"
    
    echo "/sync/newdir" > "$setup_dir/remote/.sync_exclude"
    
  else
    echo "No setup instructions for test $1. Exiting..."
    exit 1
  fi
}


# $1 is the number of the test to grade
grade_test () {
  if [[ ! -e "$expected_outputs/expected_$1" ]]; then
    echo "ERROR -- $expected_outputs/expected_$1 not found. Skipping..."
    return
  fi
  
  cmp -s "$expected_outputs/actual" "$expected_outputs/expected_$1"
  
  if [[ $? -eq 0 ]]; then
    echo "PASS -- Test $1"
    tests_passed=$((tests_passed+1))
  else
    echo "FAIL -- Test $1"
    printf "\nEXPECTED\n"
    cat "$expected_outputs/expected_$1"
    printf "\nACTUAL\n"
    cat "$expected_outputs/actual"
    echo ""
  fi 
}


if [[ $1 == "-t" ]]; then
  # Running one test
  set_up_test $2
  
  read -p "Test $2 set up. Grade now? [y/n] " grade
  echo ""
  if [[ $grade == "y" ]]; then
    $user_home/Code/Bash/filesync/filesync.sh $setup_dir/local $setup_dir/remote -v -t > "$expected_outputs/actual"
    grade_test $2
    echo ""
  else
    $user_home/Code/Bash/filesync/filesync.sh $setup_dir/local $setup_dir/remote -v
  fi
else
  # Running all tests
  for test_num in $(find "$expected_outputs" -mindepth 1 -print | grep -o "expected_.*" | sort | awk -F '_' '{print $2}'); do
    set_up_test $test_num
    $user_home/Code/Bash/filesync/filesync.sh $setup_dir/local $setup_dir/remote -v -t > "$expected_outputs/actual"
    grade_test $test_num
  done
  
  printf "\n$tests_passed/$total_tests tests passed\n"
fi
