package com.gmail.nossr50;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class Tree {

	TreeNode root = null;

	public Tree(){}

	public void add(String p, int in)
	{
		if(root == null)
			root = new TreeNode(p, in);
		else
			root.add(p,in);
	}
	
	public PlayerStat[] inOrder()
	{
		ArrayList<PlayerStat> order = root.inOrder(new ArrayList<PlayerStat>());
		return order.toArray(new PlayerStat[order.size()]);
	}

}