package vn.notiguard;
interface IGuardService {
    void destroy() = 16777114;
    String inspect(String config) = 1;
    String startProtection(String config, String baseline) = 2;
    String stopProtection(String baseline) = 3;
    String status() = 4;
    void requestCheck() = 5;
}
