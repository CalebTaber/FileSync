#!/bin/bash

# Script parameters
# $1 is the local directory
# $2 is the remote directory
# $3 can be set to -v for verbose logging
# $4 can be set to -t for running tests

# If file $1/.sync_exclude exists, the paths listed in it will be excluded from being
# synced. Useful for not copying libraries and system-specific files
# Paths listed must START with a '/' but NOT END with '/'

local_prefix="$1"
remote_prefix="$2"

# Set last sync to be the older last_sync file between the two directories
# This will ensure that the staler directory will always get the proper updates
# if the fresher directory has more recently synced with a different directory
last_sync="$local_prefix/.last_sync"
if [[ "$remote_prefix/.last_sync" -nt "$last_sync" ]]; then
  last_sync="$remote_prefix/.last_sync"
fi

blacklist="$local_prefix/.sync_exclude"
trash="$local_prefix/.sync_trash"

# If blacklist exists only locally or remotely, copy it to both
if [[ -e "$blacklist" ]] && [[ ! -e "$remote_prefix/.sync_exclude" ]]; then
  cp "$blacklist" "$remote_prefix/"
elif [[ -e "$remote_prefix/.sync_exclude" ]] && [[ ! -e "$blacklist" ]]; then
  cp "$remote_prefix/.sync_exclude" "$local_prefix"
fi

if [[ -e "$blacklist" ]]; then
  # Update the blacklist in case it was changed in either place
  cp -u "$blacklist" "$remote_prefix/.sync_exclude"
  cp -u "$remote_prefix/.sync_exclude" "$blacklist"
fi
# Clear trash directory if exists
if [[ -e "$trash" ]]; then
    rm -r "$trash"
fi
mkdir -p "$trash/local"
mkdir -p "$trash/remote"


# Check for verbose logging ($3)
verbose_logging=0;
if [[ "$3" == "-v" ]]; then
  verbose_logging=1
fi


# Check that local directory exists
if [[ ! -d "$local_prefix" ]]; then
  echo "Error: local directory $local_prefix does not exist. Exiting"
  exit 1
fi


# Check that remote directory exists
if [[ ! -d "$remote_prefix" ]]; then
  echo "Error: remote directory $remote_prefix does not exist. Exiting"
  exit 1
fi


# $1 is the info to log
log_extra_info () {
  if [ "$verbose_logging" -eq "1" ]; then
    echo "$1"
  fi
}

# $1 is the relative parent path of the file to trash
# $2 is the file name
# $3 is the flag (0 for local, 1 for remote)
trash_file () {
  local curr_dir=$(pwd)
  
  if [[ "$3" == "0" ]]; then
    mkdir -p "$trash/local/$1"
    cd "$local_prefix"
    mv "./$1/$2" "$trash/local/$1"
  elif [[ $3 == "1" ]]; then
    mkdir -p "$trash/remote/$1"
    cd "$remote_prefix"
    mv "./$1/$2" "$trash/remote/$1"
  fi
  
  cd "$curr_dir"
}

# $1 is the relative path of the directory to sync
recursive_directory_sync () {
  local filenames=()
  
  # Pipe errors from find to /dev/null because the local or remote path may not exist, which is not
  # an error in this program
  mapfile -t filenames < <( find "$local_prefix/$1" "$remote_prefix/$1" -mindepth 1 -maxdepth 1 -printf "%f\n" 2>/dev/null | sort | uniq )
  
  local num_files=${#filenames[@]}
  for index in $(seq 0 $((num_files - 1))); do
    local file_name=${filenames[index]}
    if [[ "$file_name" == ".last_sync" ]] || [[ "$file_name" == ".sync_trash" ]] || [[ "$file_name" == ".sync_exclude" ]]; then
      continue
    fi
    
    local local_path="$local_prefix/$1/$file_name"
    local remote_path="$remote_prefix/$1/$file_name"
    
    # Check if the current file is in the blacklist (removing duplicate slashes first)
    if [[ -e "$blacklist" ]] && [[ ! -z $(grep -E "^$(echo "/$1/$file_name" | sed 's/\/\/*/\//g')$" "$blacklist") ]]; then
      continue
    fi
    
    # If file exists locally and remotely
    if [[ -e "$local_path" ]] && [[ -e "$remote_path" ]]; then
    
      # If both are regular files
      if [[ -f "$local_path" ]] && [[ -f "$remote_path" ]]; then
        if [[ "$local_path" -nt "$last_sync" ]] && [[ "$local_path" -nt "$remote_path" ]]; then
          log_extra_info "Trash remote $1/$file_name and replace with local"
          trash_file "$1" "$file_name" 1
          cp $local_path "$remote_prefix/$1"
        elif [[ "$remote_path" -nt "$last_sync" ]] && [[ "$remote_path" -nt "$local_path" ]]; then
          log_extra_info "Trash local $1/$file_name and replace with remote"
          trash_file "$1" "$file_name" 0
          cp $remote_path "$local_prefix/$1"
        fi
      
      # If both are directories
      elif [[ -d "$local_path" ]] && [[ -d "$remote_path" ]]; then
        recursive_directory_sync "$1/$file_name"
      
      # If one is a file and one is a directory, or at least one is not a regular file or directory
      else
        echo "Remote and local files are non-regular or not the same type: $1/$file_name"
      fi
    
    # If the file exists only locally
    elif [[ ! -e "$remote_path" ]]; then
    
      # If modified after last sync, copy to remote
      if [[ "$local_path" -nt "$last_sync" ]]; then
        if [[ -f "$local_path" ]]; then
          log_extra_info "Copy file $1/$file_name to remote"
          cp $local_path "$remote_prefix/$1/"
        elif [[ -d "$local_path" ]]; then
          log_extra_info "Copy directory $1/$file_name to remote"
          mkdir $remote_path
          recursive_directory_sync "$1/$file_name"
        else
          echo "Local file is non-regular: $local_path"
        fi
        
      # If not modified since last sync, delete locally
      else
        log_extra_info "Trash local $1/$file_name"
        trash_file "$1" "$file_name" 0
      fi
    
    # If the file exists only remotely
    elif [[ ! -e $local_path ]]; then
    
      # If modified after last sync, copy to local
      if [[ "$remote_path" -nt "$last_sync" ]]; then
        if [[ -f "$remote_path" ]]; then
          log_extra_info "Copy file $1/$file_name to local"
          cp $remote_path "$local_prefix/$1/"
        elif [[ -d "$remote_path" ]]; then
          log_extra_info "Copy directory $1/$file_name to local"
          mkdir $local_path
          recursive_directory_sync "$1/$file_name"
        else
          echo "Remote file is non-regular: $remote_path"
        fi
        
      # If not modified since last sync, delete remotely
      else
        log_extra_info "Trash remote $1/$file_name"
        trash_file "$1" "$file_name" 1
      fi
    fi
  done
}


# Start sync
recursive_directory_sync ""

# Flag to exit here when running tests
if [[ "$4" == "-t" ]]; then
  exit 0
fi

# Print out files trashed and remove permanently if user consents
if [[ ! -z $(find "$trash" -mindepth 1 -print | grep -Eo "trash/(local|remote)/.*") ]]; then 
  printf "\nFiles trashed:\n"

  find "$trash" -mindepth 1 -print | grep -Eo "trash/(local|remote)/.*"

  echo ""
  read -p "Delete these permanently? [y/n] " delete
  if [[ "$delete" == "y" ]]; then
    rm -r "$trash"
  fi
fi


# Set new last_sync
echo $(date +%s) > "$local_prefix/.last_sync"

# Copy to remote
cp "$local_prefix/.last_sync" "$remote_prefix/"


# Case 1: File exists locally and remotely
# - Compare last modified dates
#   - If local is more recent than remote, copy local file to remote
#   - If remote is more recent than local, compare remote to last sync timestamp
#     - If file last modified after last sync, copy to local
#     - Otherwise, do nothing

# Case 2: File exists locally but not remotely
# - Compare last modified date to last sync timestamp
#   - If modified after last sync, copy to remote
#   - If modified before last sync, remove locally

# Case 3: File exists remotely but not locally
# - Compare last modified date to last sync timestamp
#   - If modified after last sync, copy to local
#   - If modified before last sync, remove remotely

#Have an option for initializing syncs on a machine
#- Can just copy the last_sync_tstmp to that machine
#- Or can copy all missing files from one machine to another (need to designate one as the source of truth)


# Written by Caleb Taber in 2024
