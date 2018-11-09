# vim:ft=make
MAKEFLAGS += --no-builtin-rules

.SUFFIXES:

dirstamp:= .dirstamp

INSTALL_C:= install -C
TOUCH_R:= touch -r
TOUCH:= touch
MKDIR_P:= mkdir -p
INSTALL_D:= install -d

INSTALL_DIR_TARGETS=
PREREQ_DIR_TARGETS=
BUILD_DIR_TARGETS=
am_DIRECTORIES=
PREREQ=

prereq__DIRECTORIES= $(addsuffix /$(dirstamp), $(PREREQ_DIR_TARGETS))

build__DIRECTORIES= $(addsuffix /$(dirstamp), $(BUILD_DIR_TARGETS))

am_install__DIRECTORIES= $(addprefix $(HOME)/, $(INSTALL_DIR_TARGETS))
install__DIRECTORIES= $(addsuffix /$(dirstamp), $(am_install__DIRECTORIES))

am_DIRECTORIES+= $(prereq__DIRECTORIES)

.PHONY: all
all: $(prereq__DIRECTORIES) $(build__DIRECTORIES) $(PREREQ) $(BUILD)

.PHONY: clean
clean:

.PHONY: distclean-am
distclean-am:

.PHONY: distclean
distclean: clean distclean-am

.DEFAULT_GOAL := all
.NOTPARALLEL:
