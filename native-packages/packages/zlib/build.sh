PACKAGE_VERSION="1.2.11"
PACKAGE_SRCURL="https://zlib.net/fossils/zlib-$PACKAGE_VERSION.tar.gz"
PACKAGE_SHA256="c3e5e9fdd5004393c9006509059f37916b8a3f25c9b46e33e46c764f69188f6c"

builder_step_configure() {
	CFLAGS+=" $CPPFLAGS -fPIC"
	LDFLAGS+=" -fPIC"
	"$PACKAGE_SRCDIR"/configure \
		--prefix="$PACKAGE_INSTALL_PREFIX" --static
}
