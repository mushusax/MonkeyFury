using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace Backend
{
    public class MonkeyFuryContext : IdentityDbContext<IdentityUser>
    {
        public MonkeyFuryContext(DbContextOptions<MonkeyFuryContext> options) : base(options)
        {

        }
    }


    public class User
    {
        public int UserId { get; set; }
    }
}
