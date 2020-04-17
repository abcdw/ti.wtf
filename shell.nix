let
  pkgs = import <nixpkgs> { overlays = [ (self: super: {
    jdk = super.adoptopenjdk-bin;
  }) ]; };
in
with pkgs;
mkShell {
  buildInputs = [clojure gnumake gcc];
  LD_LIBRARY_PATH = "${stdenv.cc.cc.lib}/lib";
}
