// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.*;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.NearbySmeltCommand;
import com.microsoft.Malmo.Schemas.NearbySmeltCommands;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Place commands allow agents to place blocks in the world without having to worry about inventory management.
 */
public class PlaceCommandsImplementation extends CommandBase implements ICommandHandler {
    private boolean isOverriding;

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return false;

        if (!verb.equalsIgnoreCase("place"))
            return false;

        InventoryPlayer inv = player.inventory;
        Block b = Block.getBlockFromName(parameter);
        if (b == null || b.getRegistryName() == null)
            return false;

        boolean foundBlock = false;
        for (int i = 0; i < 41 && !foundBlock; i++) {
            ItemStack s = inv.getStackInSlot(i).copy();
            if (b.getRegistryName().equals(s.getItem().getRegistryName())) {
                int selectedHotBarItem = inv.currentItem;
//                ItemStack selected = inv.getStackInSlot(selectedHotBarItem).copy();
                inv.setInventorySlotContents(selectedHotBarItem, s);

                RayTraceResult mop = Minecraft.getMinecraft().objectMouseOver;
                if (mop.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockPos pos = mop.getBlockPos().add(mop.sideHit.getDirectionVec());
                    // Can we place this block here?
                    AxisAlignedBB axisalignedbb = b.getDefaultState().getCollisionBoundingBox(player.world, pos);
                    if (axisalignedbb == null) {
                        PlayerInteractEvent event = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, mop.getBlockPos(), mop.sideHit, mop.hitVec);
                        MinecraftForge.EVENT_BUS.post(event);
                        if (!event.isCanceled()) {
                            BlockPos newPos = mop.getBlockPos().add(mop.sideHit.getDirectionVec());
                            Block newB = Block.getBlockFromItem(inv.getCurrentItem().getItem());
                            IBlockState blockType = newB.getStateFromMeta(inv.getCurrentItem().getMetadata());
                            if (player.world.setBlockState(newPos, blockType)) {
                                BlockSnapshot snapshot = new BlockSnapshot(player.world, newPos, blockType);
                                BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(snapshot, player.world.getBlockState(mop.getBlockPos()), player, EnumHand.MAIN_HAND);
                                MinecraftForge.EVENT_BUS.post(placeEvent);
                                // We set the block, so remove it from the inventory.
                                if (!player.isCreative()) {
                                    if (player.inventory.getCurrentItem().getCount() > 1)
                                        player.inventory.getCurrentItem().setCount(player.inventory.getCurrentItem().getCount() - 1);
                                    else
                                        player.inventory.mainInventory.get(player.inventory.currentItem).setCount(0);
                                }
                            }
                        }
                    }
                }
                foundBlock = true;

                inv.setInventorySlotContents(i, inv.getCurrentItem());
//                inv.setInventorySlotContents(selectedHotBarItem, selected);
            }
        }

        return true;
    }

    @Override
    public boolean parseParameters(Object params) {
        if (!(params instanceof PlaceCommands))
            return false;

        PlaceCommands pParams = (PlaceCommands) params;
        setUpAllowAndDenyLists(pParams.getModifierList());
        return true;
    }

    @Override
    public void install(MissionInit missionInit) {
    }

    @Override
    public void deinstall(MissionInit missionInit) {
    }

    @Override
    public boolean isOverriding() {
        return this.isOverriding;
    }

    @Override
    public void setOverriding(boolean b) {
        this.isOverriding = b;
    }
}