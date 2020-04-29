let
  pkgs = import <nixpkgs> { overlays = [ (self: super: {
    jdk = super.adoptopenjdk-bin;
  }) ]; };
in
with pkgs;
mkShell {
  buildInputs = [clojure gnumake gcc git curl httpie docker-compose postgresql_11];
  LD_LIBRARY_PATH = "${stdenv.cc.cc.lib}/lib";
  PGUSER = "postgres";
  PGPASSWORD = "postgres";
  PGHOST = "localhost";
}
