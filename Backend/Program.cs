using Backend;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

/*Build Web API*/
var builder = WebApplication.CreateBuilder(args);
var folder = Environment.SpecialFolder.LocalApplicationData;
var path = Environment.GetFolderPath(folder);
var dbPath = Path.Join(path, "monkeyfury.db");
builder.Services.AddDbContext<MonkeyFuryContext>(options => options.UseSqlite($"Data Source={dbPath}"))
    .AddAuthorization()
    .AddIdentityApiEndpoints<IdentityUser>()
    .AddEntityFrameworkStores<MonkeyFuryContext>();
var app = builder.Build();


/*Routing*/
app.MapIdentityApi<IdentityUser>();
app.Run();