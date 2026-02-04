# BetterSlowdown
This plugin fixes an issue on 1.8.9 where players skip the hit slowdown, or get a hit slowdown while not sprinting.

## Slowdown
A hit slowdown happens every time you are hitting while doing either:

- **Sprinting**
- Using a knockback sword

Now when you hit someone while meeting one of these conditions you will have a **40% motion** reduction

## The issue

When you hit someone internal server code will do **EntityPlayer#setSprinting(false)**, this causes your metadata to be updated, which in turn causes a new **metadata packet** to be sent.

This metadata packet causes your sprinting value to be overriden for a single tick for hit slowdown. (It does not override other parts of the code as the sprinting is set afterwards by key input). Due to latency this packet can arrive on any client tick and is completely inaccurate. 

Another issue with the internal server code is when you send START_SPRINTING or STOP_SPRINTING to the server, it sends another metadata packet to you with your claimed sprint metadata. This is also completely inaccurate due to latency.

## The fix

The plugin will cancel / remove any metadata updates for sprint.
Due to the sprint state being stored in a bitmask the plugin can't always cancel / remove it, there is however a config option to select what strategy to follow if the plugin can't do the cancellation. 

Doing this fix should make hit slowdown **less latency** dependant.

**Warning:** This plugin will make hit slowdown happen completely different to most servers

## Attribute
BetterSlowdown also cancels "useless" attribute packets which apply / remove the sprint modifier, this is handled client side and the server should not try to override this. 

## No slowdown
You can also use the plugin to disable hit slowdown by spamming metadata sprinting packets to the client. (Note if a client lag spikes its possible they miss the packet and still have slowdown hits)

**Warning:** this breaks dynamic fov when you toggle your sprint

## Dependencies

This plugin uses, and depends on [PacketEvents](https://github.com/retrooper/packetevents)
