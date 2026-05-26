PACKAGE_VERSION="1.2.11"
PACKAGE_SRCURL="https://zlib.net/fossils/zlib-$PACKAGE_VERSION.tar.gz"
PACKAGE_SHA256="c3e5e9fdd5004dcb542feda5ee4f0ff0744628baf8ed2dd5d66f8ca1197cb1a1"

builder_step_configure() {
	CFLAGS+=" $CPPFLAGS -fPIC"
	LDFLAGS+=" -fPIC"
	"$PACKAGE_SRCDIR"/configure \
		--prefix="$PACKAGE_INSTALL_PREFIX" --static
}
