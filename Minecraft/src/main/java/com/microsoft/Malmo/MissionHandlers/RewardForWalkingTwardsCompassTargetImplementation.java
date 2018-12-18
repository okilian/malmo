package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForDistanceTraveledToCompassTarget;


import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

public class RewardForWalkingTwardsCompassTargetImplementation extends RewardBase
{
    RewardForWalkingTwardsCompassTarget params;
    float previousDistance;
    float totalReward;

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForWalkingTwardsCompassTarget))
            return false;

        this.params = (RewardForWalkingTwardsCompassTarget)params;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        BlockPos spawn = player.world.getSpawnPoint();
        BlockPos playerLoc = player.getPosition();
        this.previousDistance = playerLoc.getDistance(spawn.getX(), spawn.getY(), spawn.getZ());

        this.totalReward = 0;

        return true;
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward)
    {
        boolean sendReward = false;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        BlockPos spawn = player.world.getSpawnPoint();
        BlockPos playerLoc = player.getPosition();

        float delta = playerLoc.getDistance(spawn.getX(), spawn.getY(), spawn.getZ()) - previousDistance;

        switch (this.params.getDensity()) {
        case MISSION_END:
            this.totalReward += this.params.getRewardPerBlock().floatValue() * delta;
            sendReward = reward.isFinalReward();
            break;
        case PER_TICK:
            this.totalReward = this.params.getRewardPerBlock().floatValue() * delta;
            sendReward = true;
            break;
        case PER_TICK_ACCUMULATED:
            this.totalReward += this.params.getRewardPerBlock().floatValue() * delta;
            sendReward = true;
            break;
        default:
            break;
        }

        this.previousDistance = playerLoc.getDistance(spawn.getX(), spawn.getY(), spawn.getZ());

        super.getReward(missionInit, reward);
        if (sendReward)
        {
            float adjusted_reward = adjustAndDistributeReward(this.totalReward, this.params.getDimension(), this.params.getRewardDistribution());
            reward.add(this.params.getDimension(), adjusted_reward);
        }
    }
}
