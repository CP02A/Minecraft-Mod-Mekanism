-- Run by the computers that ComputerCraftTests labels "mekanism_test" (the ROM's autorun scripts run on every computer), which are placed next to
-- a basic energy cube storing 1000 energy. The result is reported through the computer's label (at most 32 characters): "ok", or what failed
if os.getComputerLabel() ~= "mekanism_test" then
    return
end

local function check()
    local cube = peripheral.find("basicEnergyCube")
    if not cube then
        return "no energy cube peripheral"
    end
    if cube.getEnergy() ~= 1000 then
        return "energy " .. tostring(cube.getEnergy())
    end
    if not (cube.getMaxEnergy() > 1000) then
        return "max " .. tostring(cube.getMaxEnergy())
    end
    cube.setRedstoneMode("LOW")
    if cube.getRedstoneMode() ~= "LOW" then
        return "mode " .. tostring(cube.getRedstoneMode())
    end
    if pcall(cube.setRedstoneMode, "NOT_A_MODE") then
        return "invalid mode accepted"
    end
    if type(mekanismFilterHelper) ~= "table" then
        return "no mekanismFilterHelper"
    end
    return "ok"
end

local ok, result = pcall(check)
os.setComputerLabel(ok and result or ("error " .. tostring(result)))
