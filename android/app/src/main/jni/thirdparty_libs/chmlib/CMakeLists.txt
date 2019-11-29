
project(chmlib)

set(CHM_SRC_DIR ../../thirdparty/chmlib)

include_directories(.)

set(CHM_SRC_FILES
    ${CHM_SRC_DIR}/src/chm_lib.c
    ${CHM_SRC_DIR}/src/lzx.c
)

add_library(chmlib STATIC ${CHM_SRC_FILES})
