# LogMgmt
Application used for managing log files generated from various applications.  This tool, combined with the Linux logrotate scripts, were created for a customer with a legal obligation to never (well, almost never) actually delete a log file.  
## Download and Build the Source
* Minimum requirements:
    * Java Development Kit (v1.8.0 or higher)
    * GIT (v1.7 or higher)
    * Maven (v3.3 or higher)
* Download source
```
# cd /var/local
# git clone https://github.com/carpenlc/LogMgmt.git
```
* Build the source
```
# cd LogMgmt
# mvn clean package install
```
