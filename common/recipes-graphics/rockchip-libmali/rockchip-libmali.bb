# Copyright (C) 2019, Fuzhou Rockchip Electronics Co., Ltd
# Released under the MIT license (see COPYING.MIT for the terms)

DESCRIPTION = "Userspace Mali GPU drivers for Rockchip SoCs"
SECTION = "libs"

LICENSE = "CLOSED"
LIC_FILES_CHKSUM = "file://END_USER_LICENCE_AGREEMENT.txt;md5=3918cc9836ad038c5a090a0280233eea"

SRC_URI = " \
	git://github.com/rockchip-linux/libmali.git;branch=master; \
"
SRCREV = "95c3cee3b78725833e6e7d6f933f397ed86d32b5"
S = "${WORKDIR}/git"

DEPENDS = "libdrm patchelf-native"
DEPENDS_${PN}_px3se = "openssl10"
RDEPENDS_${PN}_px3se = "libssl libcrypto"

PROVIDES += "virtual/egl virtual/libgles1 virtual/libgles2 virtual/libgles3 virtual/libopencl virtual/libgbm"

PACKAGE_ARCH = "${MACHINE_ARCH}"

python () {
    if not d.getVar('RK_MALI_LIB'):
        raise bb.parse.SkipPackage('RK_MALI_LIB is not specified!')

    pn = d.getVar('PN')
    pn_dev = pn + "-dev"
    d.setVar("DEBIAN_NOAUTONAME_" + pn, "1")
    d.setVar("DEBIAN_NOAUTONAME_" + pn_dev, "1")

    for p in (("libegl", "libegl1"),
	      ("libgles1", "libglesv1-cm1"),
	      ("libgles2", "libglesv2-2"),
	      ("libgles3",), ("libopencl",)):
        pkgs = " ".join(p)
        d.appendVar("RREPLACES_" + pn, pkgs)
        d.appendVar("RPROVIDES_" + pn, pkgs)
        d.appendVar("RCONFLICTS_" + pn, pkgs)

        pkgs = p[0] + "-dev"
        d.appendVar("RREPLACES_" + pn_dev, pkgs)
        d.appendVar("RPROVIDES_" + pn_dev, pkgs)
        d.appendVar("RCONFLICTS_" + pn_dev, pkgs)
}

inherit cmake

do_install () {
	install -m 0755 -d ${D}/${libdir}

	if echo ${TUNE_FEATURES} | grep -wq arm; then
		cd ${S}/lib/arm-linux-gnueabihf/
	else
		cd ${S}/lib/aarch64-linux-gnu/
	fi

	install -m 0644 ${RK_MALI_LIB} ${D}/${libdir}/libMali.so.1
	patchelf --set-soname "libMali.so.1" ${D}/${libdir}/libMali.so.1

	ln -sf libMali.so.1 ${D}/${libdir}/${RK_MALI_LIB}
	ln -sf libMali.so.1 ${D}/${libdir}/libMali.so
	ln -sf libMali.so.1 ${D}/${libdir}/libEGL.so.1
	ln -sf libEGL.so.1 ${D}/${libdir}/libEGL.so
	ln -sf libMali.so.1 ${D}/${libdir}/libGLESv1_CM.so.1
	ln -sf libGLESv1_CM.so.1 ${D}/${libdir}/libGLESv1_CM.so
	ln -sf libMali.so.1 ${D}/${libdir}/libGLESv2.so.2
	ln -sf libGLESv2.so.2 ${D}/${libdir}/libGLESv2.so
	ln -sf libMali.so.1 ${D}/${libdir}/libOpenCL.so.1
	ln -sf libOpenCL.so.1 ${D}/${libdir}/libOpenCL.so
	ln -sf libMali.so.1 ${D}/${libdir}/libgbm.so.1
	ln -sf libgbm.so.1 ${D}/${libdir}/libgbm.so

	install -d -m 0755 ${D}${libdir}/pkgconfig
	install -m 0644 ${WORKDIR}/build/egl.pc ${D}${libdir}/pkgconfig/
	install -m 0644 ${WORKDIR}/build/gbm.pc ${D}${libdir}/pkgconfig/
	install -m 0644 ${WORKDIR}/build/glesv2.pc ${D}${libdir}/pkgconfig/
	install -m 0644 ${WORKDIR}/build/mali.pc ${D}${libdir}/pkgconfig/
	install -m 0644 ${WORKDIR}/build/OpenCL.pc ${D}${libdir}/pkgconfig/

	if echo ${RK_MALI_LIB} | grep -q wayland; then
		ln -sf libMali.so.1 ${D}/${libdir}/libwayland-egl.so.1
		ln -sf libwayland-egl.so.1 ${D}/${libdir}/libwayland-egl.so

		install -m 0644 ${WORKDIR}/build/wayland-egl.pc \
			${D}${libdir}/pkgconfig/
	fi

	install -d -m 0755 ${D}${includedir}
	cp -r ${S}/include/* ${D}${includedir}/
}

do_install_append_px3se() {
	install -m 0755 -d ${D}/${sysconfdir}/init.d
	install -m 0755 ${S}/overlay/S10libmali_px3se \
		${D}/${sysconfdir}/init.d/mali.sh
	install -m 0755 -d ${D}/${bindir}
	install -m 0755 ${S}/overlay/px3seBase ${D}/${bindir}/

	patchelf --replace-needed "libcrypto.so.1.0.0" "libcrypto.so.1.0.2" \
		${D}/${libdir}/libMali.so.1
	patchelf --replace-needed "libcrypto.so.1.0.0" "libcrypto.so.1.0.2" \
		${D}/${bindir}/px3seBase
	patchelf --replace-needed "libssl.so.1.0.0" "libssl.so.1.0.2" \
		${D}/${bindir}/px3seBase
}

inherit update-rc.d

# Optional initial script
INITSCRIPT_NAME = "mali.sh"
INITSCRIPT_PARAMS = "start 10 5 4 3 2 ."

INSANE_SKIP_${PN} = "already-stripped ldflags dev-so"

INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"

FILES_${PN} = " \
	${libdir}/lib*${SOLIBS} \
"
FILES_${PN}-dev = " \
	${libdir}/lib*${SOLIBSDEV} \
	${includedir} \
	${libdir}/pkgconfig \
"
FILES_${PN}_px3se = " \
	${libdir} \
	${sysconfdir} \
	${bindir} \
"
FILES_${PN}-dev_px3se = " \
	${includedir} \
	${libdir}/pkgconfig \
"

RPROVIDES_${PN} += "libmali"
